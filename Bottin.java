import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

 public class Bottin {
    public static void main(String[] args) throws Exception {
    DocumentBuilderFactory factory = 
     DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder parser = 
     factory.newDocumentBuilder();
    Document doc = parser.parse("bottin.xml");
    Element racine = doc.getDocumentElement();
    NodeList nl = racine.getChildNodes();
    if(args[0].equals("efface")) {
       for (int k = 0; k < nl.getLength(); ++k) {
          if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
             Element e = (Element) nl.item(k);
                if(e.getAttribute("nom").equals(args[1])) {
                   e.getParentNode().removeChild(e);
               }
          }
       }      
    } else if (args[0].equals("cherche")) {
     for (int k = 0; k < nl.getLength(); ++k) {
        if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
           Element e = (Element) nl.item(k);
           if(e.getAttribute("nom").equals(args[1])) {
              System.out.println(e.getAttribute("téléphone"));
           }
        }
     }      
    } else if (args[0].equals("ajoute")) {
     boolean ajout = false;
       for (int k = 0; k < nl.getLength(); ++k) {
          if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
             Element e = (Element) nl.item(k);
             if(e.getAttribute("nom").equals(args[1])) {
                e.setAttribute("téléphone",args[2]);
                ajout=true;
             }
          }
       }
       if( ! ajout) {
         Element p = doc.createElement("personne");
         p.setAttribute("nom", args[1]);
         p.setAttribute("téléphone", args[2]);
         racine.appendChild(p);
     }
    }
    TransformerFactory tfact = TransformerFactory.newInstance();
    Transformer transformer = tfact.newTransformer();
    transformer.setOutputProperty("encoding", "ISO-8859-1");
    DOMSource source = new DOMSource(doc);
    FileWriter fw = new FileWriter("bottin.xml");
    StreamResult result = new StreamResult(fw);    
    transformer.transform(source, result);
    }
 }
