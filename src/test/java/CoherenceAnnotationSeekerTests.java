import com.tangosol.dev.introspect.ClassAnnotationSeeker;
import com.tangosol.dev.introspect.ClassPathResourceDiscoverer;
import com.tangosol.io.pof.schema.annotation.PortableType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.RepeatedTest;
import org.junitpioneer.jupiter.Stopwatch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.*;

public class CoherenceAnnotationSeekerTests {

	private static final String BASE_DIRECTORY = "lib/";
	private static final String COHERENCE_JAR_FILE = BASE_DIRECTORY + "coherence-24.03.1.jar";
	private static final String PORTABLE_TYPES_JAR_FILE = BASE_DIRECTORY + "portable-types.jar";

	@RepeatedTest(5)
	@Stopwatch
	public void findPortableTypesUsingCoherenceClassAnnotationSeeker() throws Exception {
		final File coherenceJarFile = new File(COHERENCE_JAR_FILE);
		final File portableTypesJarFile = new File(PORTABLE_TYPES_JAR_FILE);

		assertThat(coherenceJarFile.isFile()).isTrue();
		assertThat(portableTypesJarFile.isFile()).isTrue();

		final URI coherenceJarFileUri = new URI("jar", coherenceJarFile.toURI() + "!/", null);
		final URI portableTypesJarFileUri = new URI("jar", portableTypesJarFile.toURI() + "!/", null);

		final List<URL> urls = List.of(coherenceJarFileUri.toURL(), portableTypesJarFileUri.toURL());

		final ClassAnnotationSeeker.Dependencies simpleDeps = new ClassAnnotationSeeker.Dependencies();
		simpleDeps.setDiscoverer(new ClassPathResourceDiscoverer.InformedResourceDiscoverer(
				urls.toArray(new URL[0])));

		final ClassAnnotationSeeker simpleSeeker  = new ClassAnnotationSeeker(simpleDeps);
		final Set<String> classNamesWithPortableTypes = simpleSeeker.findClassNames(PortableType.class);
		assertThat(classNamesWithPortableTypes).hasSize(5);
	}

	@RepeatedTest(5)
	@Stopwatch
	public void findPortableTypesUsingJandex() throws Exception {

		final File coherenceFile = new File(COHERENCE_JAR_FILE);
		final File portableTypesFile = new File(PORTABLE_TYPES_JAR_FILE);

		assertThat(coherenceFile.isFile()).isTrue();
		assertThat(portableTypesFile.isFile()).isTrue();

		final JarFile coherenceJarFile = new JarFile(coherenceFile);
		final JarFile portableTypesJarFile = new JarFile(portableTypesFile);

		final List<JarFile> jarFiles = List.of(coherenceJarFile, portableTypesJarFile);

		final List<InputStream> classInputStreams = jarFiles
				.stream()
				.flatMap(jarFile -> getClassNames(jarFile).stream())
				.toList();

		final Indexer indexer = new Indexer();

		for (InputStream inputStream : classInputStreams) {
			indexer.index(inputStream);
		}

		final Index index = indexer.complete();

		final DotName portableTypes = DotName.createSimple(PortableType.class);
		final List<AnnotationInstance> annotations = index.getAnnotations(portableTypes);

		assertThat(annotations).hasSize(5);

        for (InputStream inputStream : classInputStreams) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

	private List<InputStream> getClassNames(JarFile jarFile) {
		return jarFile.stream().map(jarEntry -> {
			if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                try {
                    return jarFile.getInputStream(jarEntry);
                }
				catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
			return null;
		})
		.filter(Objects::nonNull)
		.toList();
	}

	@RepeatedTest(5)
	@Stopwatch
	public void findPortableTypesUsingClassGraph() {
		final ClassGraph classGraph = new ClassGraph();

		classGraph.overrideClasspath(
				COHERENCE_JAR_FILE + File.pathSeparatorChar +
				PORTABLE_TYPES_JAR_FILE
		);

		classGraph.enableClassInfo();
		classGraph.enableAnnotationInfo();

        final ClassInfoList classNamesWithPortableTypes;
        try (ScanResult scanResult = classGraph.scan()) {
            scanResult.getClassesWithAnnotation(PortableType.class);
            classNamesWithPortableTypes = scanResult.getClassesWithAnnotation(PortableType.class);
        }
        assertThat(classNamesWithPortableTypes).hasSize(5);

	}
}
