

<maliste>
        {for $c in distinct-values(doc("etu.xml")//cours/@sigle)
return <cours sigle="{$c}">{avg(doc("etu.xml")//cours[@sigle=$c]/@note)}</cours>
         }
</maliste>


    