#include<stdio.h>
#include<string.h>


/* Prog C qui fournit les reponses à l'exercice tab2d.oef .
L'executable doit s'appeler tab2d.o et être place dans
~/wims/other/bin/bioinfo
Ce prog doit etre lancé par le script /wims/public_html/bin/sh_bioinfo
*/

/* Entrées :
	argv[1]= nom du programme ()
	argv[2]= \niveau
	argv[3]= \TAILLE
	argv[4]= \var_boucle1
	argv[5]= \val_ligne
	argv[6]= \diago
	argv[7]= \val_diago
	argv[8]= \boucle4_1
	argv[9]= \boucle4_2
	argv[10]= \max
	argv[11]= \l1
	argv[12]= \c1
	argv[13]=\ligne
	argv[14]=\sens_boucle1_1
	argv[15]=\sens_boucle1_2
*/

int main(int argc, char *argv[])
{
	int tab[16][16];
	int i,j,k;
	int niveau,TAILLE,var_boucle1,val_ligne,diago,val_diago,max,ligne;
	int l1,c1;
	char boucle4_1[8];
	char boucle4_2[8];
	char sens_boucle1_1[12];
	char sens_boucle1_2[12];

	sscanf(argv[2],"%d",&niveau);
	sscanf(argv[3],"%d",&TAILLE);
	sscanf(argv[4],"%d",&var_boucle1);
	sscanf(argv[5],"%d",&val_ligne);
	sscanf(argv[6],"%d",&diago);
	sscanf(argv[7],"%d",&val_diago);
	strcpy(boucle4_1,argv[8]);
	strcpy(boucle4_2,argv[9]);
	sscanf(argv[10],"%d",&max);
	sscanf(argv[11],"%d",&l1);
	sscanf(argv[12],"%d",&c1);
	sscanf(argv[13],"%d",&ligne);
	strcpy(sens_boucle1_1,argv[14]);
	strcpy(sens_boucle1_2,argv[15]);


	/* BOUCLE 1 */

	if ((strcmp(sens_boucle1_1,"croissant")==0) &&  (strcmp(sens_boucle1_2,"croissant")==0) )
	{
		if (var_boucle1<3) {
			for(i=0; i<TAILLE; i++){
				for(j=0; j<TAILLE; j++) {
					if (var_boucle1==1)
						tab[i][j]=i;
					else
						tab[i][j]=j;
				}
			}
		}
		else {
			k=0;
			for(i=0; i<TAILLE; i++) {
				for(j=0; j<TAILLE; j++) {
					tab[i][j]=k;
					k++;
				}
			}
		}
	}

	else if ((strcmp(sens_boucle1_1,"croissant")==0) && ( strcmp(sens_boucle1_2,"decroissant"))==0)
	{
		if (var_boucle1<3) {
			for(i=0; i<TAILLE; i++){
				for(j=TAILLE-1; j>=0; j--) {
					if (var_boucle1==1)
						tab[i][j]=i;
					else
						tab[i][j]=j;
				}
			}
		}
		else {
			k=0;
			for(i=0; i<TAILLE; i++) {
				for(j=TAILLE-1; j>=0; j--) {
					tab[i][j]=k;
					k++;
				}
			}
		}
	}

	else if ((strcmp(sens_boucle1_1,"decroissant")==0 &&  strcmp(sens_boucle1_2,"croissant"))==0)
	{
		if (var_boucle1<3) {
			for(i=TAILLE-1; i>=0; i--){
				for(j=0; j<TAILLE; j++) {
					if (var_boucle1==1)
						tab[i][j]=i;
					else
						tab[i][j]=j;
				}
			}
		}
		else {
			k=0;
			for(i=TAILLE-1; i>=0; i--){
				for(j=0; j<TAILLE; j++) {
					tab[i][j]=k;
					k++;
				}
			}
		}
	}

	else
	{
		if (var_boucle1<3) {
			for(i=TAILLE-1; i>=0; i--){
				for(j=TAILLE-1; j>=0; j--) {
					if (var_boucle1==1)
						tab[i][j]=i;
					else
						tab[i][j]=j;
				}
			}
		}
		else {
			k=0;
			for(i=TAILLE-1; i>=0; i--){
				for(j=TAILLE-1; j>=0; j--) {
					tab[i][j]=k;
					k++;
				}
			}
		}
	}


	/* BOUCLE 2 */
	if (niveau>1)
	{
		for(i=0; i<TAILLE; i++) {
			tab[ligne][i]=val_ligne;
			tab[i][ligne+1]=val_ligne+1;
		}
	}

	/* BOUCLE 3*/
	if (niveau>2)
	{
		if (diago==1)
		{
			for(i=0; i<TAILLE; i++) {
				tab[i][i]=val_diago;
			}
		}
		else
		{
			for(i=0; i<TAILLE; i++) {
				tab[i][TAILLE-1-i]=val_diago;
			}
		}
	}

	/* BOUCLE 4*/
	if (niveau>3)
	{
		if(strcmp(boucle4_1,"pair")==0 && strcmp(boucle4_2,"pair")==0)
		{
			for(i=0; 2*i<TAILLE; i++) {
				for(j=0; 2*j<TAILLE ; j++) {
					tab[2*i][2*j]=0;
				}
			}

		}
		else if(strcmp(boucle4_1,"impair")==0 && strcmp(boucle4_2,"pair")==0)
		{
			for(i=0; 2*i+1<TAILLE; i++) {
				for(j=0; 2*j<TAILLE ; j++) {
					tab[2*i+1][2*j]=0;
				}
			}

		}
		else if(strcmp(boucle4_1,"impair")==0 && strcmp(boucle4_2,"impair")==0)
		{
			for(i=0; 2*i+1<TAILLE; i++) {
				for(j=0; 2*j<TAILLE ; j++) {
					tab[2*i+1][2*j+1]=0;
				}
			}

		}
		else if(strcmp(boucle4_1,"pair")==0 && strcmp(boucle4_2,"impair")==0)
		{
			for(i=0; 2*i<TAILLE; i++) {
				for(j=0; 2*j<TAILLE ; j++) {
					tab[2*i][2*j+1]=0;
				}
			}

		}
	}

	/* BOUCLE 5*/
	if (niveau>4)
	{
		for(i=0;i<TAILLE;i++)
		{
			for(j=0;j<TAILLE;j++) {
				if(i+j>max)
					tab[i][j]=tab[i][j]-1;

			}
		}
	}

	printf("%d,%d,%d,%d\n",tab[l1][c1],tab[l1+1][c1+1],tab[l1+1][c1+2],tab[l1+2][c1]);
	return 0;




}
