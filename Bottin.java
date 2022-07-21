import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;



 public class Bottin {


   // Main method
   public static void main(String[] args) throws Exception {
      DocumentBuilderFactory factory = 
       DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder parser = 
       factory.newDocumentBuilder();

      // Nom du fichier XML a lire
      String filename = "bottin.xml";

      Document doc = parser.parse(filename);
      Element racine = doc.getDocumentElement();
      NodeList nl = racine.getChildNodes();
      
      // Paramètre "efface" ------------------------------------------
      if(args[0].equals("efface")) {
         for (int k = 0; k < nl.getLength(); ++k) {
            if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
               Element e = (Element) nl.item(k);
                  if(e.getAttribute("nom").equals(args[1])) {
                     e.getParentNode().removeChild(e);
                 }
            }
         }      
      } 
      // Paramètre "a" -------------------------------------------------
      else if(args[0].equals("a")) { System.out.println("Vous avez entrer a comme paramètre!");  } 
      
      // Paramètre "cherche" ------------------------------------------
      else if (args[0].equals("cherche")) {
       
          
         for (int k = 0; k < nl.getLength(); ++k) {
         //Parcours la liste
          if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
            // Créer un élément "noeud" courant 
            Element e = (Element) nl.item(k);
             // Compare avec le String args[1]
             if(e.getAttribute("nom").equals(args[1])) {
                System.out.println(e.getAttribute("telephone"));
             }
          }
       }      
      } 
      // Paramètre "ajoute" --------------------------------------------
      else if (args[0].equals("ajoute")) {
       boolean ajout = false;
         for (int k = 0; k < nl.getLength(); ++k) {
            if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
               Element e = (Element) nl.item(k);
               if(e.getAttribute("nom").equals(args[1])) {
                  e.setAttribute("telephone",args[2]);
                  ajout=true;
               }
            }
      }
         if( ! ajout) {
           Element p = doc.createElement("personne");
           p.setAttribute("nom", args[1]);
           p.setAttribute("telephone", args[2]);
           racine.appendChild(p);
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
      // -------------------------------------------------- Fin Main --------------------------------------------------
      }


 }
