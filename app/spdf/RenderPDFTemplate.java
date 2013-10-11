package spdf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;

/**
 * 200 OK
 */
public class RenderPDFTemplate extends Result {

	private static final long serialVersionUID = 6238738409770109140L;

    private MultiPDFDocuments docs;

	public RenderPDFTemplate(MultiPDFDocuments docs, Map<String, Object> args) throws TemplateNotFoundException {
    	this.docs = docs;
    	renderDocuments(args);
    }

    private void renderDocuments(Map<String, Object> args) {
    	for(PDFDocument doc : docs.documents){
    		Request request = Http.Request.current();
        	String templateName = PDF.resolveTemplateName(doc.template, request, request.format);
            Template template = TemplateLoader.load(templateName);
            doc.args.putAll(args);
    		doc.content = template.render(new HashMap<String, Object>(doc.args));
    	}
	}
    
    public void apply(Request request, Response response) {
        try {
            response.setHeader("Content-Disposition", "inline; filename=\"" + docs.filename + "\"");
            setContentTypeIfNotSet(response, "application/pdf");
            // FIX IE bug when using SSL
            if(request.secure && isIE(request))
            	response.setHeader("Cache-Control", "");

            renderPDF(response.out, request, response);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

	private boolean isIE(Request request) {
		if(!request.headers.containsKey("user-agent"))
			return false;
		
		Header userAgent = request.headers.get("user-agent");
		return userAgent.value().contains("MSIE");
	}

	private void renderPDF(OutputStream out, Request request, Response response) throws Exception {
        Map<?,?> properties = Play.configuration;
        String uri = request.getBase()+request.url;
        if(docs.documents.size() == 1){
        	renderDoc(docs.documents.get(0), uri, properties, out);
        }else{
        	// we need to concatenate them all
        	Document resultDocument = new Document();
        	PdfCopy copy = new PdfCopy(resultDocument, out);
        	resultDocument.open();
        	ByteArrayOutputStream os = new ByteArrayOutputStream();
        	for(PDFDocument doc : docs.documents){
        		os.reset();
        		renderDoc(doc, uri, properties, os);
        		PdfReader pdfReader = new PdfReader(os.toByteArray());
        		int n = pdfReader.getNumberOfPages();
        		for(int i=0;i<n;i++){
        			copy.addPage(copy.getImportedPage(pdfReader, i+1));
        		}
        		copy.freeReader(pdfReader);
        	}
        	resultDocument.close();
        }
	}
	
	private void renderDoc(PDFDocument doc, String uri, Map<?,?> properties,
			OutputStream out) throws Exception {
		PDF.toStream(doc.content, out);
	}

	public void writePDF(OutputStream out, Request request, Response response) {
        try {
            renderPDF(out, request, response);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }


}
