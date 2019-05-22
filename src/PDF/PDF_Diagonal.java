package PDF;



import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;

class PDF_Diagonal implements PdfPCellEvent {
	 /*
	  * dodatkowe formatowanie komórek (przekreœlenie)
	  * 
	  */
	 
    @Override
	public void cellLayout(PdfPCell cell, Rectangle position,
        PdfContentByte[] canvases) {
        PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, 
            new Phrase(""), position.getRight(2), position.getTop(12), 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, 
            new Phrase(""), position.getLeft(2), position.getBottom(2), 0);
        canvas = canvases[PdfPTable.LINECANVAS];
        canvas.moveTo(position.getLeft(), position.getTop());
        canvas.lineTo(position.getRight(), position.getBottom());
        canvas.stroke();
        
        canvas.moveTo(position.getRight(), position.getTop());
        canvas.lineTo(position.getLeft(), position.getBottom());
        canvas.stroke();
    }
}
