import java.io.BufferedReader;  
import java.io.FileReader;  
import java.io.IOException;
import org.w3c.dom.*;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Inventaire  
{  

   

   private static Document getDocument(String nomdocument) throws Exception 
   {
       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       factory.setNamespaceAware(true);
       DocumentBuilder builder = factory.newDocumentBuilder();
       Document doc = builder.parse(nomdocument);
       return doc;
   }

   

public static void main(String[] args)   
{  

// Création de inventairedoc

    
String line = "";  
try   
{  
// Récupère le paramètre du fichier CSV et le lit
// le fichier CSV est un fichier texte qui contient des données séparées par des virgules
BufferedReader lelecteur = new BufferedReader(new FileReader(args[0]));

// Lit tant que l'on a pas atteint la fin du fichier
while ((line = lelecteur.readLine()) != null)    
{  
String[] listeclient = line.split(",");    // use comma as separator  
// Client, # carte, code produit et quantité
//System.out.println("Nom: " + listeclient[0]);
//System.out.println("# Carte: " + listeclient[1]);
//Données de mise à jour -----------------------------------------------------
//System.out.println("Code produit " + listeclient[2]);
//System.out.println("Quantité: " + listeclient[3]);
System.out.println("Nous retirons " + listeclient[3] + " unités du produit identifié par " + listeclient[2]);

Document inventairedoc = getDocument(args[1]);
Element racine = inventairedoc.getDocumentElement();
    NodeList nl = racine.getChildNodes();
    for (int k = 0; k < nl.getLength(); ++k) {
        //Parcours la liste
         if(nl.item(k).getNodeType()==Node.ELEMENT_NODE) {
           // Créer un élément "noeud" courant 
           Element e = (Element) nl.item(k);
            // Compare avec le String args[1]
            if(e.getAttribute("nom").equals(listeclient[3])) {
               System.out.println(e.getAttribute("quantite"));
                e.setAttribute("quantite", listeclient[2]);
            }
         }
      }}  



}   
catch (IOException e)   
{  
e.printStackTrace();  
} catch (Exception e1) {
    e1.printStackTrace();
}  



// Fin main ---------------------------------------------------------------
}  
}  
