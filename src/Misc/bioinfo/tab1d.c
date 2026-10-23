#include<stdio.h>
#include<string.h>


/* Prog C qui fournit les reponses a l'exercice .oef .
L'executable doit s'appeler tab1d.o et etre place dans
~/wims/other/bin
Ce prog doit etre lance par le script /wims/public_html/bin/sh_bioinfo
*/

/* Entrees :
	argv[1]= nom du programme ()
	argv[2]= \niveau
	argv[3]= \TAILLE
	argv[4]= \sens_boucle1
	argv[5]= \valeur_boucle1
	argv[6]= \parite
	argv[7]= \val
	argv[8]= \max
	argv[9]= \q1

*/

int main(int argc, char *argv[])
{
	int tab[51];

	int i,q1,niveau, TAILLE, val, max, compteur;
	char sens_boucle1[10];
	char valeur_boucle1[10];
	char parite[10];

	sscanf(argv[2],"%d",&niveau);
	sscanf(argv[3],"%d",&TAILLE);
	strcpy(sens_boucle1,argv[4]);
	strcpy(valeur_boucle1,argv[5]);
	strcpy(parite,argv[6]);
	sscanf(argv[7],"%d",&val);
	sscanf(argv[8],"%d",&max);
	sscanf(argv[9],"%d",&q1);
	compteur =0;

	if (strcmp(sens_boucle1,valeur_boucle1)==0)
	{
		for (i=0;i<TAILLE ; i++)
		{
			tab[i] = i;
			compteur++;
		}
	}
	else
	{
		for (i=0;i<TAILLE ; i++)
		{
			tab[i] = TAILLE-i-1;
			compteur++;
		}
	}

	if(niveau>2)
	{
		for (i=1;i<TAILLE ; i++)
		{
			if (tab[i] > max)
			{
				tab[i]=tab[i-1];
			}
			else if (i> max)
			{
				tab[i]=tab[i]+1;
			}
			compteur++;

		}
	}

	if(niveau>1)
	{
		if (strcmp(parite,"pair")==0)
		{
			for (i=0; 2*i<TAILLE ;i=i+1)
			{
				tab[2*i]=tab[2*i]+val;
				compteur++;
			}
		}
		else
		{
			for (i=0; 2*i+1<TAILLE ;i=i+1)
			{
				tab[2*i+1]=tab[2*i+1]+val;
				compteur++;
			}
		}
	}

	if (niveau<4)
		compteur=0;
	printf("%d,%d,%d\n",tab[q1], tab[q1+1], compteur);


	return 0;

}


