mvn install -e;
version=$(cat pom.xml | grep -oPm1 "(?<=<version>)[^<]+");
cp ~/.m2/repository/fr/leomelki/LoupGarou/$version/LoupGarou-$version-shaded.jar ./LoupGarou-v$version-shaded.jar
