/* Cette fonction va chercher un document XML sur
le serveur et appelle la fonction afficherTitres lorsque
c'est fait! */
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
    /* Cette fonction est appelé lorsque le document XML est 
    chargé */
    function afficheTitres(doc) {
          titres = doc.getElementsByTagName("title");
          elementol = document.createElement("ol");
          var longueur = titres.length;
          for ( k = 0; k &lt; longueur ; ++k) {
            elementli = document.createElement("li");
            elementli.appendChild(
              document.createTextNode(
                titres[k].firstChild.nodeValue
              )
            );
            elementol.appendChild(elementli);
          }
          body = document.getElementsByTagName("body").item(0);
          body.appendChild(elementol);
    }