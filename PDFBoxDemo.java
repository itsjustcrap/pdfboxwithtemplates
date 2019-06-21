package pdfboxdemo;
// needs to have Apache's pdfbox2.0.15 and fontbox2.0.15
// jars in your build/run path

/*
generate multi-page PDFs and apply a singple page template to
all pages of final document (header/footer, etc)

The code in this file - and this file only - is released to you under the 
terms of the GPLv2.  Do whatever you like with it under those conditions.
Please see the LICENSE file.
 */
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

public class PDFBoxDemo {

    public static void main(String[] args) {

        int numPages = 5; 
        String FILEDIR = "/storage/pdftest";

        PDDocument thePDF = new PDDocument();

        try {

            // we'll need a stream to write to later
            PDPageContentStream pgContStr = null;

            for (int i = 0; i < numPages; i++) {

                thePDF.addPage(new PDPage());

                PDPage workingPage = thePDF.getPage(i);

                pgContStr = new PDPageContentStream(thePDF, workingPage,
                        PDPageContentStream.AppendMode.APPEND,
                        true);

                pgContStr.beginText();
                pgContStr.setFont(PDType1Font.TIMES_ROMAN, 24);
                pgContStr.newLineAtOffset(250, 250);
                String text = "This is page " + (i + 1) + "!";
                pgContStr.showText(text);
                pgContStr.endText();
                pgContStr.close();
            }

            File templateFile = new File(FILEDIR + "/letterhead.pdf");
            PDDocument templateDocument = null;
            PDPage templatePage = null;
            templateDocument = PDDocument.load(templateFile);
            templatePage = templateDocument.getPage(0);

            PDPageTree destinationPages = thePDF.getDocumentCatalog().getPages();
            LayerUtility layerUtility = new LayerUtility(thePDF);
            PDFormXObject firstForm = layerUtility.importPageAsForm(templateDocument, 0);
            AffineTransform affineTransform = new AffineTransform();
            for (int i = 0; i < numPages; i++) {
                PDPage destPage = destinationPages.get(i);
                layerUtility.wrapInSaveRestore(destPage);
                layerUtility.appendFormAsLayer(destPage, firstForm, affineTransform, "p" + i);
            }
            templateDocument.close();

            // now lock pdf
            String uPwd = ""; // user password - use none and it Just Opens
            String oPwd = "supersecretblendofspices";  // the "owner" password to provide edits,etc.
            int keyLen = 128; // supposedly supports 256 but it gave me an Exception
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(oPwd, uPwd, ap);
            spp.setEncryptionKeyLength(keyLen);
            spp.setPermissions(ap);
            // the actual permissions
            ap.setCanAssembleDocument(false);
            ap.setCanExtractContent(false);            
            ap.setCanFillInForm(false);
            ap.setCanModify(false);
            ap.setCanModifyAnnotations(false);
            ap.setCanExtractForAccessibility(true); // needed for screen readers, etc
            ap.setCanPrint(true);
            ap.setCanPrintDegraded(true);
            thePDF.protect(spp);
            // done locking

            long unixTime = System.currentTimeMillis() / 1000L;
            thePDF.save(FILEDIR + "/output_" + unixTime + ".pdf");
            System.out.println("File saved to: " + FILEDIR + "/output_" + unixTime + ".pdf");
            thePDF.close();
        } catch (IOException ex) {
            System.out.println("*** IOException: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
