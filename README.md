# Booking-Reservation-System

Let's try the simplest solution first:

    Update your pom.xml with the one I provided above

    Run these commands:
 # Clean everything
    mvn clean

# Download all dependencies
    mvn dependency:resolve

# Compile
    mvn compile

# Run with classpath
    mvn exec:java -Dexec.mainClass="jabulile.httpapi.BookingAppServer" -Dexec.classpathScope="runtime"

If that doesn't work, try running it differently:

# ######
# Package first
mvn package

# Then run the class directly with proper classpath
java -cp "target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" jabulile.httpapi.BookingAppServer


# ######
Solution 2: Alternative - Run with Maven exec plugin

Instead of mvn exec:java, create a runnable JAR:
# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/booking-reservation-system-1.0-SNAPSHOT.jar


If you get "no main manifest attribute", use this:
# Run with classpath
java -cp "target/classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':')" jabulile.httpapi.BookingAppServer