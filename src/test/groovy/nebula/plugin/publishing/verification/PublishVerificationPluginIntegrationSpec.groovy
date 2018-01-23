package nebula.plugin.publishing.verification

import nebula.plugin.dependencylock.DependencyLockPlugin
import nebula.plugin.publishing.ivy.IvyPublishPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.resolutionrules.ResolutionRulesPlugin
import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.DependencyGraphBuilder
import nebula.test.dependencies.DependencyGraphNode
import nebula.test.dependencies.GradleDependencyGenerator
import nebula.test.dependencies.ModuleBuilder
import nebula.test.functional.ExecutionResult
import netflix.nebula.dependency.recommender.DependencyRecommendationsPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin

class PublishVerificationPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        settingsFile.text = '''\
            rootProject.name='testhello'
        '''
    }

    def 'should successful pass through verification'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'snapshot'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksSuccessfully('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        result.standardOutput.contains(":verifyPublication")
        result.standardOutput.contains(":publishNebulaIvyPublicationToDistIvyRepository")
    }

    def 'should fail when any library status is less then published project status'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with dependency lock'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        runTasks('generateLock', 'saveLock')
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with dependency recommendation plugin'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile 'foo:bar'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)
        buildFile << """           
            dependencyRecommendations {
                 map recommendations: ['foo:bar': '1.0-SNAPSHOT']
            }
        """

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with resolution rules plugin using replace rule'() {
        given:
        def expectedFailureDependency = 'replace-new:replace-new:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule('replace-original:replace-original:1.0-SNAPSHOT')
        builder.addModule(expectedFailureDependency)
        DependencyGraphNode somethingWithNew = new ModuleBuilder('something-with-new:something-with-new:1.5')
                .addDependency(expectedFailureDependency).build()
        builder.addModule(somethingWithNew)
        def dependencies = """
            compile 'replace-original:replace-original:1.0-SNAPSHOT'
            compile 'something-with-new:something-with-new:1.5'
        """

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with resolution rules plugin using alignment rule'() {
        given:
        def expectedFailureDependency = 'align-group:align-part1:1.1-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule('align-group:align-part1:1.0-SNAPSHOT')
        builder.addModule(expectedFailureDependency)
        builder.addModule('align-group:align-part2:1.1-SNAPSHOT')
        def dependencies = """
            compile 'align-group:align-part1:1.0-SNAPSHOT'
            compile 'align-group:align-part2:1.1-SNAPSHOT'
        """

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with resolution rules plugin using substitution rule'() {
        given:
        def expectedFailureDependency = 'substitute-new:substitute-new:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule('substitute-original:substitute-original:1.1-SNAPSHOT')
        builder.addModule(expectedFailureDependency)
        builder.addModule('substitute-new:substitute-new:1.1-SNAPSHOT')
        def dependencies = "compile 'substitute-original:substitute-original:1.1-SNAPSHOT'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with resolution rules plugin using reject rule'() {
        given:
        def expectedFailureDependency = 'reject:reject:1.0.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        builder.addModule('reject:reject:1.1.0-SNAPSHOT')
        def dependencies = "compile 'reject:reject:1.+'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with maven publish'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'publishNebulaPublicationToDistMavenRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with artifactoryPublish'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'artifactoryPublish')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with artifactoryDeploy'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        def dependencies = "compile '$expectedFailureDependency'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksWithFailure('build', 'artifactoryDeploy')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'should work with forced dependency'() {
        given:
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        builder.addModule('foo:bar:1.0')
        def dependencies = "compile 'foo:bar:1.0'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)
        buildFile << """
            configurations.all {
                resolutionStrategy {
                    force '$expectedFailureDependency'
                }
            }
        """

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    def 'test runtime configuration should not be checked'() {
        given:
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule('foo:bar:1.0-SNAPSHOT')
        builder.addModule('foo:bar:1.0')
        def dependencies = """
            compile 'foo:bar:1.0'
            testCompile 'foo:bar:1.0-SNAPSHOT'
        """

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksSuccessfully('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        result.standardOutput.contains(":verifyPublication")
        result.standardOutput.contains(":publishNebulaIvyPublicationToDistIvyRepository")
    }

    def 'only first level dependencies are verified'() {
        given:
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        def module = new ModuleBuilder('foo:bar:1.0').addDependency('baz:buz:1.0-SNAPSHOT').build()
        builder.addModule(module)
        def dependencies = "compile 'foo:bar:1.0'"

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksSuccessfully('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        result.standardOutput.contains(":verifyPublication")
        result.standardOutput.contains(":publishNebulaIvyPublicationToDistIvyRepository")
    }

    def 'ignored dependencies are not verified'() {
        given:
        def projectStatus = 'release'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule('foo:bar:1.0-SNAPSHOT')
        builder.addModule('baz:bax:1.0-SNAPSHOT')
        def dependencies = """
             compile nebulaPublishVerification.ignore('foo:bar:1.0-SNAPSHOT')
             compile nebulaPublishVerification.ignore(group: 'baz', name: 'bax', version: '1.0-SNAPSHOT')
        """

        buildFile << createBuildFileFromTemplate(projectStatus, dependencies, builder)

        when:
        def result = runTasksSuccessfully('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        result.standardOutput.contains(":verifyPublication")
        result.standardOutput.contains(":publishNebulaIvyPublicationToDistIvyRepository")
    }

    def 'should work with java-library plugin'() {
        given:
        def projectStatus = 'release'
        def expectedFailureDependency = 'foo:bar:1.0-SNAPSHOT'
        DependencyGraphBuilder builder = new DependencyGraphBuilder()
        builder.addModule(expectedFailureDependency)
        DependencyGraph graph = builder.build()
        def generator = new GradleDependencyGenerator(graph)
        File mavenRepoDir = generator.generateTestMavenRepo()

        buildFile << """           
            ${applyPlugin(IvyPublishPlugin)}
            ${applyPlugin(MavenPublishPlugin)}
            ${applyPlugin(ResolutionRulesPlugin)}
            ${applyPlugin(DependencyLockPlugin)}
            ${applyPlugin(DependencyRecommendationsPlugin)}
            apply plugin: 'java-library'
         
            group = 'test.nebula.netflix'
            status = '$projectStatus'            
            version = '1.0'
            
                       
            repositories {
                maven {
                    url "file://$mavenRepoDir.canonicalPath"
                }
            }
           
            dependencies {
                compile '$expectedFailureDependency'
            } 
            
            ${publishingRepos()}
        """

        settingsFile.text = '''\
            rootProject.name='testhello'
        '''

        when:
        def result = runTasksWithFailure('build', 'publishNebulaIvyPublicationToDistIvyRepository')

        then:
        assertFailureMessage(result, expectedFailureDependency, projectStatus)
    }

    private String createBuildFileFromTemplate(String projectStatus, String dependencies, DependencyGraphBuilder builder) {
        DependencyGraph graph = builder.build()
        def generator = new GradleDependencyGenerator(graph)
        File mavenRepoDir = generator.generateTestMavenRepo()

        File jsonRulesFile = new File(projectDir, 'local-rules.json')
        String rules = this.getClass().getResourceAsStream('/nebula/plugin/publishing/verification/recommendation-rules.json').text
        jsonRulesFile.text = rules

        """           
            ${applyPlugin(IvyPublishPlugin)}
            ${applyPlugin(MavenPublishPlugin)}
            ${applyPlugin(ResolutionRulesPlugin)}
            ${applyPlugin(DependencyLockPlugin)}
            ${applyPlugin(DependencyRecommendationsPlugin)}
            ${applyPlugin(ArtifactoryPlugin)}          
            apply plugin: 'java'

            group = 'test.nebula.netflix'                       
            version = '1.0'
            status = '${projectStatus}' 
            
                       
            repositories {
                maven {
                    url "file://$mavenRepoDir.canonicalPath"
                }
            }
           
            dependencies {
                resolutionRules files('${jsonRulesFile}')
                ${dependencies}
            } 

            ${publishingRepos()}
        """
    }

    private String publishingRepos() {
        """
        publishing {
            repositories {
                ivy {
                    name 'distIvy'
                    url project.file("\${project.buildDir}/distIvy").toURI().toURL()
                }
                maven {
                    name 'distMaven'
                    url project.file("\${project.buildDir}/distMaven").toURI().toURL()
                }
            }
        }
        """
    }

    private void assertFailureMessage(ExecutionResult result, String expectedFailureDependency, String projectStatus) {
        assert result.standardError.contains("Module '$expectedFailureDependency' cannot be used because it has status: 'integration' which is less then your current project status: '$projectStatus' in your status scheme: [integration, milestone, release]")
    }
}