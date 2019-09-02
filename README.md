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




## The sprocket label

The sprocket label is on the form
skatteetaten.no/sprocket=sha1-<sha1hex of docker.registry/group/name:tag>

If you try to set this via echo remember to strip newlines and if you use jq use the -j switch.

Examples for how to label from some resources below
    
### imageStreams
oc label is $1 skatteetaten.no/sprocket=sha1-$(oc get is $1 -o json | jq -j ".spec.tags[].from.name" | sha1sum | cut -d' ' -f1) --overwrite

### Deployments
kubectl label deployment $1 skatteetaten.no/sprocket=sha1-$(kubectl get deployment $1 -o json | jq -j ".spec.template.spec.containers[].image" | sha1sum | cut -d' ' -f1) --overwrite
