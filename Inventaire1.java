import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;



 public class Inventaire1 {


    public static void modifier(String nomdoc, String codeproduitvendu, int quantitevendue) throws Exception {

        DocumentBuilderFactory factory = 
        DocumentBuilderFactory.newInstance();
       factory.setNamespaceAware(true);
       DocumentBuilder parser = 
        factory.newDocumentBuilder();
   
       // Nom du fichier XML a lire
       String filename = nomdoc;
   
       Document doc = parser.parse(filename);
       Element racine = doc.getDocumentElement();
       NodeList nl = racine.getChildNodes();
       Boolean vrai = true;
       // Paramètre "cherche" ------------------------------------------
       if (vrai == true) {
           for (int k = 0; k < nl.getLength(); ++k) {
           //Parcours la liste
            if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
              // Créer un élément "noeud" courant 
              Element e = (Element) nl.item(k);
               // Récupère le code et la quantité
   
               int quantite =  Integer. parseInt(e.getAttribute("quantite"));
               if(e.getAttribute("code").equals(codeproduitvendu)) {
               // On récupère la quantité vendue
                 System.out.println(e.getAttribute("quantite") + " est la quantité de " + e.getAttribute("code"));
                  // Modifie la quantité ------------------------------------------
                  String quantiterestante = Integer.toString(quantite - quantitevendue);
                  e.setAttribute("quantite", quantiterestante);
                  System.out.println(e.getAttribute("quantite") + " est maintenant la quantité de " + e.getAttribute("code"));
                 
               //------------------------------------------------------
               }  
               }
            }
         }      
       // Bloc important -------------------------------------------------
       TransformerFactory tfact = TransformerFactory.newInstance();
       Transformer transformer = tfact.newTransformer();
       transformer.setOutputProperty("encoding", "ISO-8859-1");
       DOMSource source = new DOMSource(doc);
       FileWriter fw = new FileWriter(filename);
       StreamResult result = new StreamResult(fw);    
       transformer.transform(source, result);

    }


   // Main method
   public static void main(String[] args) throws Exception {
    
    modifier(args[0], "321", 4);
   
    // -------------------------------------------------- Fin Main --------------------------------------------------
    }

}
