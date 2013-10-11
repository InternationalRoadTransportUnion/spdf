package spdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;

import play.Play;
import play.libs.IO;
import spdf.PDF;

public class PDFTest {

  @Test
  public void testSimpleGeneration() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    Play.applicationPath = File.createTempFile("/tmp", "test");
    PDF.toStream("<html><head></head><body>test</body></html>", baos);
    
    assertTrue(baos.size() > 0);
    assertEquals("%PDF-1.", baos.toString().substring(0, 7));
    IO.copy(new ByteArrayInputStream(baos.toByteArray()), new FileOutputStream("/tmp/test.pdf"));
  }
}