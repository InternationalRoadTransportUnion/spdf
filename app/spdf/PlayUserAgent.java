package spdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;

import play.Logger;
import play.Play;
import play.vfs.VirtualFile;

import com.lowagie.text.Image;

public class PlayUserAgent extends ITextUserAgent {

	private final ITextOutputDevice _outputDevice;
	
	public PlayUserAgent(ITextOutputDevice outputDevice) {
		super(outputDevice, ITextRenderer.DEFAULT_DOTS_PER_PIXEL);
		this._outputDevice = outputDevice;
	}

	@Override
	public ImageResource getImageResource(String uri) {
		Logger.debug("Fetching image '%s'", uri);

		// TODO: make it work
		InputStream stream = Play.classloader.getResourceAsStream(uri);
		if (stream != null) {
			try {
				Image image = Image.getInstance(getData(stream));
				scaleToOutputResolution(image);
				return new ImageResource(uri, new ITextFSImage(image));
			} catch (Exception e) {
				Logger.error("fetching image " + uri, e);
				throw new RuntimeException(e);
			}
		} else {
			return super.getImageResource(uri);
		}
	}

	@Override
	public CSSResource getCSSResource(String uri) {
		Logger.debug("Fetching CSS '%s'", uri);
		try {
			// uri is in fact a complete URL
			String path = new URL(uri).getPath();

			VirtualFile virtualFile = VirtualFile.fromRelativePath(path.substring(1));
			if (virtualFile != null) {
				return new CSSResource(virtualFile.inputstream());
			} else {
				return super.getCSSResource(uri);
			}
		} catch (MalformedURLException e) {
			Logger.error("fetching css " + uri, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] getBinaryResource(String uri) {
		Logger.debug("Fetching binary '%s'", uri);
		try {
			String path; 
			if (uri.startsWith("/")) {
				path = uri;
			} else {
				path = new URL(uri).getPath();
			}

			VirtualFile virtualFile = VirtualFile.fromRelativePath(path.substring(1));
			if (virtualFile != null) {
				return virtualFile.content();
			} else {
				return super.getBinaryResource(uri);
			}
		} catch (MalformedURLException e) {
			Logger.error("fetching binary " + uri, e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public XMLResource getXMLResource(String uri) {
		Logger.debug("Fetching binary '%s'", uri);

		// TODO: make it work
		InputStream stream = Play.classloader.getResourceAsStream(uri);
		if (stream != null) {
			return XMLResource.load(stream);
		} else {
			return super.getXMLResource(uri);
		}
	}

	private void scaleToOutputResolution(Image image) {
		float factor = _outputDevice.getSharedContext().getDotsPerPixel();
		image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
	}

	private byte[] getData(InputStream stream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(stream, baos);
		return baos.toByteArray();
	}

	private void copy(InputStream stream, OutputStream os) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int len = stream.read(buffer);
			os.write(buffer, 0, len);
			if (len < buffer.length)
				break;
		}
	}
}