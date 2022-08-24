import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

// Permet d'ouvrir une fenêtre sous windows:
import java.awt.Desktop;
import java.net.URI;
  

public class ledevoir {

    // --------------------------------------------------------------
    public static void ouvrir_nav(String URI) throws Exception{
        Desktop desk = Desktop.getDesktop();
        desk.browse(new URI(URI));
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

      


    // XML TO AJAX--------------------------------------------------------------
    public static void rendre_lisible(String nom_fichier_a_modifier) throws SAXException, IOException, ParserConfigurationException, TransformerException {
    // Pour rendre le fichier lisible, il faut:
    // 1. Récupérer les information importantes (nodeList)
            // 1.1. Récupérer <channel><title>
            // 1.2. Récupérer <item><title>
            // 1.3. Récupérer <item><description>
            // 1.4. Récupérer <item><link>
            // 1.5. Récupérer <item><pubdate>
    // 2. Effacer le document
    // 3. Configurer les balises HTML de base et les informations importantes
    DocumentBuilderFactory factory = 
        DocumentBuilderFactory.newInstance();
       DocumentBuilder parser = 
        factory.newDocumentBuilder();
       Document doc = parser.parse(nom_fichier_a_modifier);
       
       Element racine = doc.getDocumentElement();
       System.out.println(racine.getNodeName()); 
       // Affiche RSS
       String channel = "channel";
        String titre = "title";
        String description = "description";
        String link = "link";
        String pubdate = "pubdate";
        String item = "item";

        NodeList NLracine = racine.getElementsByTagName(channel);
        // 1.1 Affiche <channel><title>
        // System.out.println("TextContent " + element_enfant.getTextContent());
        NodeList channel_liste = noeud_enfant(NLracine, titre);
        // 1.2
        NodeList item_liste = noeud_enfant(NLracine, item);
        NodeList titre_item_liste = noeud_enfant(item_liste, titre);
        NodeList description_item_liste = noeud_enfant(item_liste, description);

        // Fonction Affiche le contenu d'une NodeList (Nodelist NodeList_a_afficher))
        affiche_noeud_for(titre_item_liste);
        affiche_noeud_for(description_item_liste);

       

      }



      



       
       //System.out.println(racine.getNodeValue());
       //NodeList nl = racine.getElementsByTagName("joueur");
       //for (int i = 0; i < nl.getLength(); ++i) {
         // Element joueur = (Element) nl.item(i);
          //NodeList listedenoms = joueur.getElementsByTagName("nom");
          //Element nom = (Element) listedenoms.item(0);
          //System.out.println(nom.getFirstChild().getNodeValue());
       //}
    // ----------------------------------------------------------------------------------------------------------------------



    private static NodeList noeud_enfant(NodeList nl, String nom_tagname_enfant) {
        //Tant qu'il y a des node dans nl
         for (int i = 0; i < nl.getLength(); i++) {
        //Va cherche 1 à 1 les element de la liste nl  
        Element element = (Element) nl.item(i);
          // Va cherche le titre du channel sous forme de noeud
          NodeList liste_enfant = element.getElementsByTagName(nom_tagname_enfant);
          // Passe de noeud à element
        //affiche_noeud(liste_enfant, i);
          return liste_enfant;
    }return null;}

    private static void affiche_noeud(NodeList nl, int i) {
        // Passe de noeud à element selon l'index
          Element element_enfant = (Element) nl.item(i);
          //On utilise l'element
            System.out.println("NodeName: " + element_enfant.getNodeName());
            System.out.println("TextContent " + element_enfant.getTextContent());
    }
    private static void affiche_noeud_for(NodeList nl) {
        for (int i = 0; i < nl.getLength(); ++i) {
        // Passe de noeud à element selon l'index
          Element element_enfant = (Element) nl.item(i);
          //On utilise l'element
            System.out.println("NodeName: " + element_enfant.getNodeName());
            System.out.println("TextContent " + element_enfant.getTextContent());
    }}


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
        String emplacement = "file://C:/Users/LT/Documents/GitHub/XMLTeluqNote/recopy.xml";
        int a=1;
        int b=0;


        afficher(b);
        b = somme(a, b);
        afficher(b);
        
        // Charger fichier xml grâce URI
        charger_uri(URI, nomsortant);

        // Modifier l'encodage pour éviter un problème lié aux caractères spéciaux
        // Effectue une simple modification du fichier d'un point de vue .text
        modifier_encodage(nomsortant);
        
        //Inutile mais permet de comparer copy.xml et recopy.xml lors du développement/évaluation
        charger_uri(nomsortant, nom_document_modifier);
        rendre_lisible(nom_document_modifier);

        // Ouvrir dans une fenêtre
        // --------------------------------------------------------------
       ouvrir_nav(emplacement);
        // ---------------------------------------------------------------------------

    }



    private static void afficher(int b) {
        System.out.println("b = " + b);
    }



    private static int somme(int a, int b) {
        b+=a+a+a;
        return b;
    }





    

}
