import java.io.BufferedReader;
import java.io.FileReader;  
import java.io.IOException;

import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;



public class Inventaire  
{  
   


    public static void modifier(String nomdoc, String codeproduitvendu, String quantitevendue) throws Exception {
        
        //System.out.println("OUI!");
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
                
               int quantite =  Integer. parseInt(e.getAttribute("quantite"))-(Integer.parseInt(quantitevendue));
               
               // On récupère la quantité vendue
                 //System.out.println(e.getAttribute("quantite") + " est la quantité de " + e.getAttribute("code"));
                  // Modifie la quantité ------------------------------------------
                    String quantiterestante = Integer.toString(quantite);
                   // System.out.println("Le code " + e.getAttribute("code"));
                   // System.out.println("Le code du fichier texte " + codeproduitvendu);
                   // String Code = (e.getAttribute("code")).replaceAll("\\s", "");
                    //Boolean test = (" "+Code == codeproduitvendu);
                   // System.out.println("Le test est " + test);

                  if(e.getAttribute("code").equals(codeproduitvendu)) {
                    e.setAttribute("quantite", quantiterestante);
                  //System.out.println(e.getAttribute("quantite") + " est maintenant la quantité de " + e.getAttribute("code"));
                  }
               //------------------------------------------------------
                
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








public static void main(String[] args) throws IOException   
{  

String line = "";  

// Récupère le paramètre du fichier CSV et le lit
// le fichier CSV est un fichier texte qui contient des données séparées par des virgules
BufferedReader lelecteur;
try {
    lelecteur = new BufferedReader(new FileReader(args[0]));

// Lit tant que l'on a pas atteint la fin du fichier
while ((line = lelecteur.readLine()) != null)    
{  
String[] listeclient = line.split(",");    // use comma as separator  
// Client, # carte, code produit et quantité
//Données de mise à jour -----------------------------------------------------
//System.out.println("Code produit " + listeclient[2]);
//System.out.println("Quantité: " + listeclient[3]);
//System.out.println("Nous retirons " + listeclient[3] + " unités du produit identifié par " + listeclient[2]);
String idproduit = listeclient[2].replaceAll("\\s", "");
String quantite = listeclient[3].replaceAll("\\s", "");
//Fonction modifier -----------------------------------------------------
modifier(args[1], idproduit, quantite);

}   
} catch (Exception e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
}



// Fin main ---------------------------------------------------------------
}  
}  
