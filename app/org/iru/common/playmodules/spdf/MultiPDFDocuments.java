package org.iru.common.playmodules.spdf;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.iru.common.playmodules.spdf.PDF.Options;

public class MultiPDFDocuments {
	public List<PDFDocument> documents = new LinkedList<PDFDocument>();
	public String filename;

	public MultiPDFDocuments(String filename) {
		this.filename = filename;
	}

	public MultiPDFDocuments() {
		// TODO Auto-generated constructor stub
	}

	public MultiPDFDocuments add(PDFDocument singleDoc) {
		documents.add(singleDoc);
		return this;
	}

	public MultiPDFDocuments add(String template, Options options,
			Object... args) {
		documents.add(new PDFDocument(template, options, args));
		return this;
	}

	public MultiPDFDocuments add(String template, Options options,
			Map<String, Object> args) {
		documents.add(new PDFDocument(template, options, args));
		return this;
	}
}