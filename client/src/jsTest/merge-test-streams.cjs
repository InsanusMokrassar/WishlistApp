// Keep Kotlin JS test process output on one pipe until KGP includes KT-69896.
// Both console methods still appear in Gradle's test reports.
process.stderr.write = process.stdout.write.bind(process.stdout);
