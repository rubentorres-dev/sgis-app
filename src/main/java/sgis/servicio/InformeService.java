package sgis.servicio;

import sgis.modelo.VentaResumen;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el informe de ventas en PDF (para guardar o imprimir).
 */
public class InformeService {

    private static final Color AZUL = new Color(20, 33, 61);
    private static final Color GRIS_CLARO = new Color(245, 246, 250);
    private static final Color BORDE = new Color(224, 227, 235);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generarInformeVentas(List<VentaResumen> ventas, LocalDate desde, LocalDate hasta, File destino)
            throws IOException {

        Font fTitulo = new Font(Font.HELVETICA, 20, Font.BOLD, AZUL);
        Font fSubtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(90, 100, 120));
        Font fSeccion = new Font(Font.HELVETICA, 12, Font.BOLD, AZUL);
        Font fEncabezado = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font fTexto = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        Font fNegrita = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
        Font fNota = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(120, 128, 154));

        double totalGeneral = 0;
        Map<String, Double> porMetodo = new LinkedHashMap<>();
        for (VentaResumen v : ventas) {
            totalGeneral += v.getTotal();
            porMetodo.merge(capitalizar(v.getMetodoPago()), v.getTotal(), Double::sum);
        }

        Document documento = new Document(PageSize.A4, 40, 40, 50, 40);
        try (FileOutputStream salida = new FileOutputStream(destino)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Informe de ventas", fTitulo));
            documento.add(new Paragraph("Supermercado Abasto La Barata", fSubtitulo));
            documento.add(new Paragraph("Periodo: " + desde.format(FECHA) + " al " + hasta.format(FECHA), fSubtitulo));
            documento.add(new Paragraph("Generado el " + LocalDateTime.now().format(FECHA_HORA), fSubtitulo));
            documento.add(new Paragraph(" "));

            // ----- Resumen -----
            documento.add(new Paragraph("Resumen", fSeccion));
            documento.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(60);
            resumen.setHorizontalAlignment(Element.ALIGN_LEFT);
            resumen.addCell(celda("Número de ventas", fTexto, Element.ALIGN_LEFT, GRIS_CLARO));
            resumen.addCell(celda(String.valueOf(ventas.size()), fNegrita, Element.ALIGN_RIGHT, GRIS_CLARO));
            resumen.addCell(celda("Total vendido", fTexto, Element.ALIGN_LEFT, GRIS_CLARO));
            resumen.addCell(celda(FormatoMoneda.formatear(totalGeneral), fNegrita, Element.ALIGN_RIGHT, GRIS_CLARO));
            for (Map.Entry<String, Double> e : porMetodo.entrySet()) {
                resumen.addCell(celda("Pagado con " + e.getKey().toLowerCase(), fTexto, Element.ALIGN_LEFT, null));
                resumen.addCell(celda(FormatoMoneda.formatear(e.getValue()), fTexto, Element.ALIGN_RIGHT, null));
            }
            documento.add(resumen);
            documento.add(new Paragraph(" "));

            // ----- Detalle -----
            documento.add(new Paragraph("Detalle de ventas", fSeccion));
            documento.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

            if (ventas.isEmpty()) {
                documento.add(new Paragraph("No hay ventas registradas en este periodo.", fTexto));
            } else {
                PdfPTable tabla = new PdfPTable(5);
                tabla.setWidthPercentage(100);
                tabla.setWidths(new float[]{8f, 22f, 30f, 20f, 20f});
                tabla.setHeaderRows(1);

                String[] titulos = {"N°", "Fecha", "Cliente", "Método de pago", "Total"};
                for (int i = 0; i < titulos.length; i++) {
                    tabla.addCell(celda(titulos[i], fEncabezado,
                            i == 4 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, AZUL));
                }

                boolean alterno = false;
                for (VentaResumen v : ventas) {
                    Color fondo = alterno ? GRIS_CLARO : null;
                    tabla.addCell(celda(String.valueOf(v.getIdVenta()), fTexto, Element.ALIGN_LEFT, fondo));
                    tabla.addCell(celda(v.getFecha().format(FECHA_HORA), fTexto, Element.ALIGN_LEFT, fondo));
                    tabla.addCell(celda(v.getNombreCliente(), fTexto, Element.ALIGN_LEFT, fondo));
                    tabla.addCell(celda(capitalizar(v.getMetodoPago()), fTexto, Element.ALIGN_LEFT, fondo));
                    tabla.addCell(celda(FormatoMoneda.formatear(v.getTotal()), fTexto, Element.ALIGN_RIGHT, fondo));
                    alterno = !alterno;
                }

                // Fila final con el total general
                PdfPCell etiqueta = celda("TOTAL", fNegrita, Element.ALIGN_RIGHT, GRIS_CLARO);
                etiqueta.setColspan(4);
                tabla.addCell(etiqueta);
                tabla.addCell(celda(FormatoMoneda.formatear(totalGeneral), fNegrita, Element.ALIGN_RIGHT, GRIS_CLARO));

                documento.add(tabla);
            }

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                    "Informe generado por Administrador de ventas. Los comprobantes de venta son recibos "
                    + "informativos y no corresponden a facturas electrónicas con validez fiscal ante la DIAN.",
                    fNota));

            documento.close();
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
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
