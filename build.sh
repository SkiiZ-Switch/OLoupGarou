version=$(cat pom.xml | grep -oPm1 "(?<=<version>)[^<]+");
mvn install;
cp /d/Dev/.m2/repository/fr/leomelki/LoupGarou/$version/LoupGarou-$version-shaded.jar /d/Dev/LoupGarou-server/plugins/LoupGarou-v$version-shaded.jar
