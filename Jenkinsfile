// pipeline {
//     agent { label 'docker' }
//     stages {
//         stage('Build') {
//             steps {
//                 sh "docker build -t ${GIT_COMMIT} ."
//             }
//         }
//         stage('Publish Latest') {
//             when {
//                 branch 'master'
//             }
//             steps {
//                 sh "docker tag ${GIT_COMMIT} fintlabsacr.azurecr.io/audit-web-eventhub:build.${BUILD_NUMBER}"
//                 withDockerRegistry([credentialsId: 'fintlabsacr.azurecr.io', url: 'https://fintlabsacr.azurecr.io']) {
//                     sh "docker push fintlabsacr.azurecr.io/audit-web-eventhub:build.${BUILD_NUMBER}"
//                 }
//             }
//         }
//         stage('Publish PR') {
//             when { changeRequest() }
//             steps {
//                 sh "docker tag ${GIT_COMMIT} fintlabsacr.azurecr.io/audit-web-eventhub:${BRANCH_NAME}.${BUILD_NUMBER}"
//                 withDockerRegistry([credentialsId: 'fintlabsacr.azurecr.io', url: 'https://fintlabsacr.azurecr.io']) {
//                     sh "docker push fintlabsacr.azurecr.io/audit-web-eventhub:${BRANCH_NAME}.${BUILD_NUMBER}"
//                 }
//             }
//         }
//         stage('Publish Version') {
//             when {
//                 tag pattern: "v\\d+\\.\\d+\\.\\d+(-\\w+-\\d+)?", comparator: "REGEXP"
//             }
//             steps {
//                 script {
//                     VERSION = TAG_NAME[1..-1]
//                 }
//                 sh "docker tag ${GIT_COMMIT} fintlabsacr.azurecr.io/audit-web-eventhub:${VERSION}"
//                 withDockerRegistry([credentialsId: 'fintlabsacr.azurecr.io', url: 'https://fintlabsacr.azurecr.io']) {
//                     sh "docker push fintlabsacr.azurecr.io/audit-web-eventhub:${VERSION}"
//                 }
//             }
//         }
//     }
// }
