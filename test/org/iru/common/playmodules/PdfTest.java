package org.iru.common.playmodules;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import junit.framework.Assert;

import org.junit.Test;

import play.libs.IO;

public class PdfTest {

  @Test
  public void testSimpleGeneration() throws FileNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Pdf.toStream("<html><head></head><body>test</body></html>", baos);
    Assert.assertEquals("%PDF-1.", baos.toString().substring(0, 7));
    IO.copy(new ByteArrayInputStream(baos.toByteArray()), new FileOutputStream("/tmp/test.pdf"));
  }
}


