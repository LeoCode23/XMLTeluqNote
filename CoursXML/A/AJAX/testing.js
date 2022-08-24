

function chargeDocument(URI) {
try {
 xmlhttp = new XMLHttpRequest();
 xmlhttp.open("GET", URI,true);
 xmlhttp.onreadystatechange=function() {
 if (xmlhttp.readyState==4) {
  afficheTitres(xmlhttp.responseXML);
  }
 }
 xmlhttp.send(null);
 } catch(o) {alert(o);}
}

function afficheTitres(doc) {
    /* Accède à l'élement racine */
    /* racine = doc.documentElement; */
    /* Affiche l'élément racine */
    /*alert(racine.nodeName);*/
    /* Accède tous les éléments item */
    var items = doc.getElementsByTagName("item");
    /* Affiche le nombre d'éléments item */
    /*alert(items.length + 1);*/

      titres = doc.getElementsByTagName("title");
      description = doc.getElementsByTagName("description");
      
      elementol = document.createElement("ol");
      var longueur = items.length + 1;
      /* Pour récupérer uniquement les éléments contenus dans les balises item, on modifie la boucle pour obtenir l'index 1. */
      for ( k = 1; k &lt; longueur; ++k) {
        
        elementli = document.createElement("li");
        elementli.appendChild(
          document.createTextNode("Titre #"+ k + ": " + titres[k].firstChild.nodeValue)
        );

        elementol.appendChild(elementli);
        elementli = document.createElement("li");
        elementli.appendChild(
          document.createTextNode("Description: " + description[k].firstChild.nodeValue)
        );
        elementol.appendChild(elementli);

      }
    


      body = document.getElementsByTagName("body").item(0);
      body.appendChild(elementol);
}
