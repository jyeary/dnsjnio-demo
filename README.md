# dnsjnio-demo

Until the builds are published to Maven Central, you may need to download the **dnsjnio-x.x.x.jar**
from the project, and install it in your local Maven repository. This can be accomplished by
using the following command where the **-Dfile=** location is the path to the jar file to install.

```bash
mvn install:install-file -Dfile=dnsjnio-1.0.4.jar -DgroupId=uk.nominet \
-DartifactId=dnsjnio -Dversion=1.0.4 -Dpackaging=jar
```
