package org.iru.common.playmodules.spdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.XMLResource;

import play.Logger;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Scope;
import play.vfs.VirtualFile;

import com.lowagie.text.pdf.BaseFont;

/**
 * Simpler version of the play PDF module.
 * 
 * Heavilly inspired by both the play 1.x pdf module and the 2.x module.
 */
public class PDF {

	public static class Options {
		public String	filename	= null;
	}

	public static void toStream(String string, OutputStream os) {
		try {
			Reader reader = new StringReader(string);
			ITextRenderer renderer = new ITextRenderer();
			renderer.getFontResolver().addFontDirectory(
			        Play.applicationPath.getPath() + "/conf/fonts", BaseFont.EMBEDDED);
			PlayUserAgent myUserAgent = new PlayUserAgent(renderer.getOutputDevice());
			myUserAgent.setSharedContext(renderer.getSharedContext());
			renderer.getSharedContext().setUserAgentCallback(myUserAgent);
			Document document = XMLResource.load(reader).getDocument();
			renderer.setDocument(document, "http://localhost:9000");
			renderer.layout();
			renderer.createPDF(os);
		} catch (Exception e) {
			Logger.error("Creating document from template", e);
			e.printStackTrace();
		}
	}

	static String resolveTemplateName(String templateName, Request request, String format) {
		if (templateName.startsWith("@")) {
			templateName = templateName.substring(1);
			if (!templateName.contains(".")) {
				templateName = request.controller + "." + templateName;
			}
			templateName = templateName.replace(".", "/") + "."
			        + (format == null ? "html" : format + ".html");
		}
		Boolean templateExists = false;
		for (VirtualFile vf : Play.templatesPath) {
			if (vf == null) {
				continue;
			}
			VirtualFile tf = vf.child(templateName);
			if (tf.exists()) {
				templateExists = true;
				break;
			}
		}
		if (!templateExists) {
			if (templateName.lastIndexOf("." + format) != -1) {
				templateName = templateName.substring(0, templateName.lastIndexOf("." + format))
				        + ".html";
			}
		}
		return templateName;
	}

	public static void renderTemplateAsPDF(OutputStream out, MultiPDFDocuments docs, Object... args) {
		Scope.RenderArgs templateBinding = Scope.RenderArgs.current();

		try {
			for (Object o : args) {
				List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
				for (String name : names) {
					templateBinding.put(name, o);
				}
			}
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}

		templateBinding.put("session", Scope.Session.current());
		templateBinding.put("request", Http.Request.current());
		templateBinding.put("flash", Scope.Flash.current());
		templateBinding.put("params", Scope.Params.current());
		try {
			templateBinding.put("errors", Validation.errors());
		} catch (Exception ex) {
			throw new UnexpectedException(ex);
		}
		try {
			if (out == null) {
				// we're rendering to the current Response object
				throw new RenderPDFTemplate(docs, templateBinding.data);
			} else {
				RenderPDFTemplate renderer = new RenderPDFTemplate(docs, templateBinding.data);
				renderer.writePDF(out, Http.Request.current(), Http.Response.current());
			}
		} catch (TemplateNotFoundException ex) {
			if (ex.isSourceAvailable()) {
				throw ex;
			}
			StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
			if (element != null) {
				throw new TemplateNotFoundException(ex.getPath(),
				        Play.classes.getApplicationClass(element.getClassName()),
				        element.getLineNumber());
			} else {
				throw ex;
			}
		}
	}

	/**
	 * Render the corresponding template.
	 * 
	 * @param args
	 *            The template data. If am {@link Options} objects is present it
	 *            is used.
	 */
	public static void renderPDF(Object... args) {
		OutputStream os = null;
		writePDF(os, args);
	}

	public static void writePDF(File file, Object... args) {
		try {
			OutputStream os = new FileOutputStream(file);
			writePDF(os, args);
			os.flush();
			os.close();
		} catch (IOException e) {
			throw new UnexpectedException(e);
		}
	}

	public static void writePDF(OutputStream out, Object... args) {
		final Http.Request request = Http.Request.current();
		final String format = request.format;

		PDFDocument singleDoc = new PDFDocument();
		MultiPDFDocuments docs = null;

		if (args.length > 0) {
			boolean firstEmpty = false;
			try {
				firstEmpty = ((List<String>) LocalVariablesNamesTracer
				        .getAllLocalVariableNames(args[0])).isEmpty();
			} catch (Exception e) {
				throw new UnexpectedException(e);
			}

			if (args[0] instanceof String && firstEmpty) {
				singleDoc.template = args[0].toString();
			} else if (args[0] instanceof MultiPDFDocuments) {
				docs = (MultiPDFDocuments) args[0];
			}
			if (docs == null) {
				for (Object arg : args) {
					if (arg instanceof Options) {
						singleDoc.options = (Options) arg;
					}
				}
			}
		}
		if (docs == null) {
			docs = new MultiPDFDocuments();
			docs.add(singleDoc);
			if (singleDoc.template == null) {
				singleDoc.template = request.action.replace(".", "/") + "."
				        + (format == null ? "html" : format + ".html");
			}
			if (singleDoc.options != null && singleDoc.options.filename != null)
				docs.filename = singleDoc.options.filename;
			else
				docs.filename = FilenameUtils.getBaseName(singleDoc.template) + ".pdf";
			System.err.format("COUCOU %s", singleDoc.template);
		}

		renderTemplateAsPDF(out, docs, args);
	}
}
