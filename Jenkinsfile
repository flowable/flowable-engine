node {
    def workspace = pwd()
    //def pom = readMavenPom
    stage('Clone repository') {
        /* Let's make sure we have the repository cloned to our workspace */
        checkout scm
    }

    stage('create rds-idm image') {
        dir('modules/flowable-ui-idm/flowable-ui-idm-app') {
            sh 'pwd'
            sh 'mvn clean install -PbuildDockerImage'
            docker.withRegistry('https://192.168.178.57:18079', 'rds-docker-nexus-credentials') {
                def gaspPublic = docker.build("gasp/idm")
                //gaspPublic.push("${pom.version}")
                gaspPublic.push("latest")
            }
        }
    }

    stage('create rds-designer image') {
        dir('modules/flowable-ui-modeler/flowable-ui-modeler-app') {
            sh 'pwd'
            sh 'mvn clean install -PbuildDockerImage'
            docker.withRegistry('https://192.168.178.57:18079', 'rds-docker-nexus-credentials') {
                def gaspInternal = docker.build("gasp/designer")
                //gaspInternal.push("${pom.version}")
                gaspInternal.push("latest")
            }
        }
    }
}