package spdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.UnexpectedException;
import spdf.PDF.Options;

public class PDFDocument {
	public String template;
	public Options options;
	public Map<String, Object> args = new HashMap<String, Object>();
	String content;

	private PDFDocument(String template, Options options) {
		this.template = template;
		this.options = options;
	}

	public PDFDocument(String template, Options options, Object... args) {
		this(template, options);
		try {
			for (Object o : args) {
				List<String> names = (List<String>) LocalVariablesNamesTracer
						.getAllLocalVariableNames(o);
				for (String name : names) {
					this.args.put(name, o);
				}
			}
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	public PDFDocument(String template, Options options,
			Map<String, Object> args) {
		this(template, options);
		this.args.putAll(args);
	}

	public PDFDocument() {
	}
}