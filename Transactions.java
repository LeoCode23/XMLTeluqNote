/**
* INF 6450 -  Travail noté 4 - Léo Talbot,  fait avec JDK 17.0.3
*/
    import org.w3c.dom.*;
    import javax.xml.parsers.*;

public class Transactions {



 public static void main(String[] args) throws Exception {
    DocumentBuilderFactory factory = 
     DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = 
     factory.newDocumentBuilder();
    Document doc = parser.parse(args[0]);


   Element racine = doc.getDocumentElement();
    
    // On créer une liste de nodes qui contient les éléments "client"
    NodeList nl = racine.getElementsByTagName("client");
    
    // On parcourt la liste de noeuds client
    for (int i = 0; i < nl.getLength(); ++i) {

      // On récupère les noeuds clients de la liste
       Element client = (Element) nl.item(i);

       
       // Affiche le nom du client
       System.out.println("Nom du client: " + client.getAttribute("nom"));
       
      // Liste des élements transaction
       NodeList transcnt = client.getElementsByTagName("transaction");
    
       int somme_total = 0;
      
      for (int a = 0; a < transcnt.getLength(); ++a) {
      int max = transcnt.getLength();
      
      // Récupération transaction
      Element transaction = (Element) transcnt.item(a);
      // Récupération l'attribut montant
      int b =  Integer.parseInt(transaction.getAttribute("montant"));
      
      // Permet d'afficher les transactions
      //System.out.println("Transaction: " + b);
      
      somme_total+=b;
      //En fin de boucle, on affiche la somme total
      if (a+1 == max) 
      {System.out.println("Somme: " + somme_total);}
      }


       


    }
  }
 }
