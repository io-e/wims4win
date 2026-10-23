#include<stdio.h>
#include<string.h>

/* Prog C qui fournit les reponses a l'exercice Etat_civil.oef .
L'executable doit s'appeler C_etat_civil.o et être place dans
~/wims/public_html/bin
Ce prog doit etre lance par le script /wims/public_html/bin/sh_etat_civil
*/

/* Entrees :
	argv[1]= nom du programme (C_etat_civil.o)
	argv[2]= \code_oui1
	argv[3]= \code_oui2
	argv[4]= \code_etat1
	argv[5]= \code_etat2
	argv[6]= \code_celibataire
	argv[7]= reponse1
	argv[8]= reponse2
	argv[9]= \nom
*/

int main(int argc, char *argv[])
{
	char code_oui1[4];
	char code_oui2[4];
	char code_celibataire[13];

	char reponse1[6];
	char reponse2[7];
	char etat[14];
	char nom[30];
	strcpy(code_oui1,argv[2]);
	strcpy(code_oui2,argv[3]);
	strcpy(code_celibataire,argv[6]);

	strcpy(reponse1,argv[7]);
	strcpy(reponse2,argv[8]);
	strcpy(nom,argv[9]);

		/* Les sauts de ligne doivent etre ecrits en html : <br> */
	printf("Etes vous de sexe masculin ?<br>");
	/*scanf("%s",reponse1);*/
	if (strcmp(reponse1,"oui") == 0 || strcmp(reponse1,code_oui1) == 0 )
		strcpy(etat,"Monsieur");
	else
	{
		printf("Etes-vous %s?<br>", code_celibataire);
		/*scanf("%s",reponse2);*/
		if (strcmp(reponse2,"oui")==0 || strcmp(reponse2,code_oui2)==0  )
			strcpy(etat,argv[4]);
		else
			strcpy(etat,argv[5]);
	}
	printf("Bonjour %s<br>",etat);
	printf("Quel est votre nom ?<br>");
	/*
	   Les saisies clavier peuvent ne contenir que deux elements
	   si sujet de sexe masculin ayant repondu correctement a la
	   premiere question. Alors
	   argv[8] contient \nom
	   argv[9] contient 0
	*/
	if (nom[0]=='0')
		printf("Au revoir %s %s<br>",etat,argv[8]);
	else
		printf("Au revoir %s %s<br>",etat,nom);

	/*puts(nom);*/
	return 0;
}





