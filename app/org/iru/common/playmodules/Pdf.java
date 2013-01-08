package org.iru.common.playmodules;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;

import play.Logger;
import play.Play;

import com.lowagie.text.Image;

public class Pdf {

  public static class MyUserAgent extends ITextUserAgent {

    public MyUserAgent(ITextOutputDevice outputDevice) {
      super(outputDevice);
    }

    @Override
    public ImageResource getImageResource(String uri) {
            InputStream stream = Play.classloader.getResourceAsStream(uri);
            if (stream != null) {
                try {
                    Image image = Image.getInstance(getData(stream));
                    scaleToOutputResolution(image);
                    return null; // TODO: return new ImageResource(new ITextFSImage(image));
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
      try {
        // uri is in fact a complete URL
        String path = new URL(uri).getPath();
            InputStream stream = Play.classloader.getResourceAsStream(uri);
                if (stream != null) {
                    return new CSSResource(stream);
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
            InputStream stream = Play.classloader.getResourceAsStream(uri);
            if (stream != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    copy(stream, baos);
                } catch (IOException e) {
                    Logger.error("fetching binary " + uri, e);
                    throw new RuntimeException(e);
                }
                return baos.toByteArray();
            } else {
                return super.getBinaryResource(uri);
            }
    }

    @Override
    public XMLResource getXMLResource(String uri) {
            InputStream stream = Play.classloader.getResourceAsStream(uri);
            if (stream!= null) {
                return XMLResource.load(stream);
            } else {
                return super.getXMLResource(uri);
            }
    }

    private void scaleToOutputResolution(Image image) {
      float factor = getSharedContext().getDotsPerPixel();
      image.scaleAbsolute(image.getPlainWidth() * factor,
          image.getPlainHeight() * factor);
    }

    private byte[] getData(InputStream stream) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      copy(stream, baos);
      return baos.toByteArray();
    }

    private void copy(InputStream stream, OutputStream os)
        throws IOException {
      byte[] buffer = new byte[1024];
      while (true) {
        int len = stream.read(buffer);
        os.write(buffer, 0, len);
        if (len < buffer.length)
          break;
      }
    }
  }

  public static void toStream(String string, OutputStream os) {
    try {
      Reader reader = new StringReader(string);
      ITextRenderer renderer = new ITextRenderer();
      //renderer.getFontResolver().addFontDirectory(Play.applicationPath.getPath() + "/conf/fonts", BaseFont.EMBEDDED);
      MyUserAgent myUserAgent = new MyUserAgent(renderer.getOutputDevice());
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
}
