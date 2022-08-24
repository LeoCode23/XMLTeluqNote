import org.w3c.dom.*;
import javax.xml.parsers.*;

//Méthode 
 public class Arbre {
    public static void traite(Node node, int i) {
      
      // Indentation -------------------------------------------------------
      // On part de rien  
      String s= ""; 
      // Et on ajoute une indentation pour chaque niveau
      //for (int k = 0; k < i; ++k) s+=" ";
       


       // Affiche nom et valeur du noeud
       // Notez la présence du s (indentation) 
       System.out.println(s+"Nom: "+ node.getNodeName() +
       " Valeur: "+node.getNodeValue());
      
       // getParentNode() renvoie le noeud parent
       // getChildNodes() renvoie la liste des noeuds fils
       // getFirstChild() renvoie le premier noeud fils
       // getLastChild() renvoie le dernier noeud fils
       // getPreviousSibling() renvoie le noeud précédent
       // getNextSibling() renvoie le noeud suivant
       // getAttributes() renvoie la liste des attributs
       // getFirstAttribute() renvoie le premier attribut
       // getLastAttribute() renvoie le dernier attribut
       // getAttributeNode(String name) renvoie l'attribut dont le nom est name
       // getAttribute(String name) renvoie la valeur de l'attribut dont le nom est name
       // getAttributeNS(String namespaceURI, String localName) renvoie la valeur de l'attribut dont le nom est localName et qui est dans l'espace de noms namespaceURI
       // getAttributeNodeNS(String namespaceURI, String localName) renvoie l'attribut dont le nom est localName et qui est dans l'espace de noms namespaceURI
         // getElementsByTagName(String name) renvoie la liste des noeuds fils dont le nom est name   
         // getElementsByTagNameNS(String namespaceURI, String localName) renvoie la liste des noeuds fils dont le nom est localName et qui est dans l'espace de noms namespaceURI
         // getElementsByTagName(String name) renvoie la liste des noeuds fils dont le nom est name
         // getElementsByTagNameNS(String namespaceURI, String localName) renvoie la liste des noeuds fils dont le nom est localName et qui est dans l'espace de noms namespaceURI
         // Permet de progresser dans l'arbre jusqu'à NULL
         NodeList listedenoeud = node.getChildNodes();
       

       if(listedenoeud != null) {
       for (int k = 0; k < listedenoeud.getLength(); ++k) {
          traite( listedenoeud.item(k),i+2);
       }
    }
 }
    public static void main(String[] args) throws Exception {
       DocumentBuilderFactory factory = 
      
       DocumentBuilderFactory.newInstance();
       DocumentBuilder parser = factory.newDocumentBuilder();

       // Définit le fichier XML à parser -----------------------------------
       String filename = args[0];


       // On parse le fichier XML
       Document doc = parser.parse(filename);
       // 2 paramètres: le fichier XML et l'index de départ
       traite(doc,0); 

    }
 }
 