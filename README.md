# Sprocket

Sprocket is a fraggle that listens to changes in docker registry and triggers new deploys if there are any changes

 ## Setup
 
 In order to use this project you must set repositories in your `~/.gradle/init.gradle` file
 
     allprojects {
         ext.repos= {
             mavenCentral()
             jcenter()
         }
         repositories repos
         buildscript {
          repositories repos
         }
     }

// TODO:

 - endepunkt for å ta i mot docker event
  - støtte for Cloud events på sikt
 - parse det til en intern data class : ImageChange
 - service som tar en ImageChange og finner AffectedResources ( type=ImageStream trigger=ImageChange)
  - typer= ImageStream, DeploymentConfig, Deployment, BuildConfig
 - ImageStreamPerformer - ImportImage
 - DeploymentPerformer - patch
 - DeploymentConfig - //har du ConfigChange trigger. Se deployment, ellers manuell deploy
 
 
Push -> docker.registry/no_skatteetaen/referanse:2
//SPROCKET_NAMESPACES=foo,bar
SPROCKET_RESOURCES=ImageStreams




TODO: Hvordan ser en event fra Dokcer Registry ut.

Hvilken algoritme skal vi hashe med

label har lengde begrensning 64 tegn.

Algortime som tar docker url -> 64 tegn. 

sha1 fungerer sha256 blir for langt
