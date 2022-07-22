import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class ledevoir {

    // --------------------------------------------------------------
    public static void navigateur(String URI) throws Exception {
        // Fonction qui ouvre un navigateur internet
        // Ref: https://mkyong.com/java/open-browser-in-java-windows-or-linux/
        String url = URI;
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        try {
            if (os.indexOf("win") >= 0) {
                // this doesn't support showing urls in the form of "page.html#nameLink"
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.indexOf("mac") >= 0) {
                rt.exec("open " + url);
            } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                // Do a best guess on unix until we get a platform independent way
                // Build a list of browsers to try, in this order.
                String[] browsers = { "epiphany", "firefox", "mozilla", "konqueror",
                        "netscape", "opera", "links", "lynx" };
                // Build a command string which looks like "browser1 "url" || browser2 "url"
                // ||..."
                StringBuffer cmd = new StringBuffer();
                for (int i = 0; i < browsers.length; i++)
                    cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + url + "\" ");
                rt.exec(new String[] { "sh", "-c", cmd.toString() });
            } else {
                return;
            }
        } catch (Exception e) {
        }
    }
    // ----------------------------------------------------------------------------------------------------------------------



    // --------------------------------------------------------------
    public static void charger_uri(String URI, String nom_document_cree) throws Exception {
        // Charger fichier grâce URI
        
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(URI);
            // Écriture d'un document
            TransformerFactory tfact = TransformerFactory.newInstance();
            Transformer transformer = tfact.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");DOMSource source = new DOMSource(doc);
            FileWriter fw = new FileWriter(nom_document_cree);
            StreamResult result = new StreamResult(fw);
            transformer.transform(source, result);
            fw.close();
        }
    // ----------------------------------------------------------------------------------------------------------------------

      


    // --------------------------------------------------------------
    public static void rendre_lisible(String nom_fichier_a_modifier) throws SAXException, IOException, ParserConfigurationException, TransformerException {
    // Pour rendre le fichier lisible, il faut:
    // 1. Retirer les balises d'entête
    // 2. Configurer les balises HTML de base
    

    
    }
    // ----------------------------------------------------------------------------------------------------------------------



    // Modifie la déclaration de l'encodage de UTF-8 à ISO-8859-1 ----------------------------------------------------- 
    // Je sais que cela ne modifie pas réellement l'encodage, mais c'est une solution à mon problème.
    static void modifier_encodage(String filePath) {
        String ancien = "encoding=\"utf-8\"";
        String nouveau = "encoding=\"ISO-8859-1\"";
        File fileToBeModified = new File(filePath);
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));
            String line = reader.readLine();
            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();}
            String newContent = oldContent.replaceAll(ancien, nouveau);
            writer = new FileWriter(fileToBeModified);
            writer.write(newContent);
        } catch (IOException e) { e.printStackTrace(); } finally {
            try {
                reader.close();
                writer.close();
            } catch (IOException e) {e.printStackTrace();}}}
        // ------------------------------------------------------


   


    // MAIN ----------------------------------------------------------------------------------------------------------------------
    public static void main(String args[]) throws Exception {
        String test = "www.google.com";
        String URI = "https://www.ledevoir.com/rss/ledevoir.xml";
        String nomsortant = "copy.xml";
        String nom_document_modifier = "recopy.xml";
        String emplacement = "file:///C:/Users/LT/Documents/GitHub/XMLTeluqNote/recopy.xml";
        

        charger_uri(URI, nomsortant);
        modifier_encodage(nomsortant);
        //Inutile mais permet de comparer copy.xml et recopy.xml lors du développement
        charger_uri(nomsortant, nom_document_modifier);
        rendre_lisible(nom_document_modifier);

        // Ouvrir dans un navigateur
        // --------------------------------------------------------------
       navigateur(emplacement);
        // ---------------------------------------------------------------------------

    }





    

}
