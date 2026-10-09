package sgis.servicio;

import sgis.modelo.DetalleVenta;
import sgis.modelo.Venta;
import sgis.util.FormatoMoneda;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Genera en PDF la factura de una venta para entregársela al cliente.
 *
 * IMPORTANTE: es un documento informativo. No es una factura electrónica con validez
 * ante la DIAN (eso requiere un proveedor tecnológico autorizado), y así se indica
 * en el propio documento.
 */
public class FacturaService {

    private static final Color AZUL = new Color(20, 33, 61);
    private static final Color GRIS_CLARO = new Color(245, 246, 250);
    private static final Color BORDE = new Color(224, 227, 235);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generarFactura(Venta venta, String nombreCliente, String correoCliente, File destino)
            throws IOException {

        Font fTitulo = new Font(Font.HELVETICA, 20, Font.BOLD, AZUL);
        Font fSeccion = new Font(Font.HELVETICA, 14, Font.BOLD, AZUL);
        Font fEncabezado = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font fTexto = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        Font fNegrita = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
        Font fTotal = new Font(Font.HELVETICA, 13, Font.BOLD, AZUL);
        Font fNota = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(120, 128, 154));

        Document documento = new Document(PageSize.A4, 40, 40, 50, 40);
        try (FileOutputStream salida = new FileOutputStream(destino)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Supermercado Abasto La Barata", fTitulo));
            documento.add(new Paragraph("Factura de venta N° " + venta.getIdVenta(), fSeccion));
            documento.add(new Paragraph("Documento informativo sin validez fiscal ante la DIAN", fNota));
            documento.add(new Paragraph(" "));

            // ----- Datos de la venta -----
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(70);
            datos.setHorizontalAlignment(Element.ALIGN_LEFT);
            filaDato(datos, "Cliente", nombreCliente, fNegrita, fTexto);
            filaDato(datos, "Correo", correoCliente, fNegrita, fTexto);
            filaDato(datos, "Fecha", venta.getFecha() == null ? "-" : venta.getFecha().format(FECHA_HORA), fNegrita, fTexto);
            filaDato(datos, "Método de pago", capitalizar(venta.getMetodoPago()), fNegrita, fTexto);
            documento.add(datos);
            documento.add(new Paragraph(" "));

            // ----- Productos -----
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{44f, 12f, 22f, 22f});
            tabla.setHeaderRows(1);

            String[] titulos = {"Producto", "Cant.", "Precio unit.", "Subtotal"};
            for (int i = 0; i < titulos.length; i++) {
                tabla.addCell(celda(titulos[i], fEncabezado,
                        i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT, AZUL));
            }

            boolean alterno = false;
            for (DetalleVenta linea : venta.getDetalles()) {
                Color fondo = alterno ? GRIS_CLARO : null;
                tabla.addCell(celda(linea.getNombreProducto(), fTexto, Element.ALIGN_LEFT, fondo));
                tabla.addCell(celda(String.valueOf(linea.getCantidad()), fTexto, Element.ALIGN_RIGHT, fondo));
                tabla.addCell(celda(FormatoMoneda.formatear(linea.getPrecioUnitario()), fTexto, Element.ALIGN_RIGHT, fondo));
                tabla.addCell(celda(FormatoMoneda.formatear(linea.getSubtotal()), fTexto, Element.ALIGN_RIGHT, fondo));
                alterno = !alterno;
            }

            PdfPCell etiqueta = celda("TOTAL A PAGAR", fTotal, Element.ALIGN_RIGHT, GRIS_CLARO);
            etiqueta.setColspan(3);
            tabla.addCell(etiqueta);
            tabla.addCell(celda(FormatoMoneda.formatear(venta.getTotal()), fTotal, Element.ALIGN_RIGHT, GRIS_CLARO));
            documento.add(tabla);

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Valores en pesos colombianos (COP). Gracias por su compra.", fTexto));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                    "Este documento es un recibo informativo de la compra realizada y no corresponde a una "
                    + "factura electrónica con validez ante la DIAN.", fNota));

            documento.close();
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private void filaDato(PdfPTable tabla, String etiqueta, String valor, Font fEtiqueta, Font fValor) {
        tabla.addCell(celda(etiqueta, fEtiqueta, Element.ALIGN_LEFT, GRIS_CLARO));
        tabla.addCell(celda(valor, fValor, Element.ALIGN_LEFT, null));
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        celda.setBorderColor(BORDE);
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        return celda;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "-";
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
