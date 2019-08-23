package cz.havranek.opensource.cacheUtils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "OptionalUsedAsFieldOrParameterType"})
public class CacheProbe {
    public static final Optional<CacheInfo> cacheInfo;

    static {
        Optional<CacheInfo> transitional;
        try {
            transitional = Optional.ofNullable(getCache());
        } catch (Exception e) {
            transitional = Optional.empty();
        }
        cacheInfo = transitional;
    }

    /**
     * this method gets cache at call time.... main purpose is to allow debugging at time of running and allow logging
     * mainly the static final variable should be accessed this is sort of last resort
     *
     * @return currently queried value
     * @throws Exception if something failed
     */

    public static CacheInfo getCache() throws Exception {
        final String os = System.getProperty("os.name").toLowerCase();
        final ShellQueryProvider shellQueryProvider;
        final ValueParser L4Parser, L3Parser, L2ParseRaw, L2ParsePerCore;
        final int toByteMultiplier;
        if (os.contains("win")) {
            shellQueryProvider = () -> {
                StringBuilder out = new StringBuilder(runProcess("cmd.exe ", "/c", "wmic cpu get L3CacheSize, L2CacheSize, NumberOfCores /format:list"));
                try {
                    out.append("\n\n").append(runProcess(true, "cmd.exe ", "/c", "wmic cpu get L4CacheSize /format:list"));
                } catch (RuntimeException ignored) {
                }
                return out.toString();
            };

            L4Parser = (data) -> extractFromStr(data, "L4CacheSize\\D*(\\d+)");
            L3Parser = (data) -> extractFromStr(data, "L3CacheSize\\D*(\\d+)");
            L2ParseRaw = (data) -> extractFromStr(data, "L2CacheSize\\D*(\\d+)");
            L2ParsePerCore = (data) -> extractFromStr(data, "L2CacheSize\\D*(\\d+)") / extractFromStr(data, "NumberOfCores\\D*(\\d+)");
            toByteMultiplier = 1024;
        } else if (os.contains("nix")||os.contains("nux")) {
            shellQueryProvider = () -> runProcess("bash", "-c", "getconf -a");
            L4Parser = (data) -> extractFromStr(data, "LEVEL4_CACHE_SIZE\\D*(\\d+)");
            L3Parser = (data) -> extractFromStr(data, "LEVEL3_CACHE_SIZE\\D*(\\d+)");
            L2ParseRaw = (data) -> extractFromStr(data, "LEVEL2_CACHE_SIZE\\D*(\\d+)");
            L2ParsePerCore = L2ParseRaw;
            toByteMultiplier = 1;
        } else if (os.contains("mac")) {
            throw new RuntimeException("querying cache on mac os not implemented yet");
        } else if (os.contains("sunos")) {
            throw new RuntimeException("querying cache on solaris os not implemented yet");
        } else {
            throw new RuntimeException("can't identify OS... Java reports OS as " + os);
        }
        return platformNeutralQuery(shellQueryProvider, L4Parser, L3Parser, L2ParseRaw, L2ParsePerCore, toByteMultiplier);
    }

    /**
     * Querry system shell while being system agnostic ... it handle all errors internally and coalesce all null and 0 values to empty optional.
     * it does allows multiply all values and multiplication should multiply to bytes
     *
     * @param shellQueryProvider provides string to be parsed by parsers
     * @param L4                 provides int of this particular level of cache parsed from string.
     * @param L3                 provides int of this particular level of cache parsed from string.
     * @param L2_FromSys         provides int of L2 cache that system reports
     * @param L2_PerCoreEst      provides int of L2 cache that is estimate of l2 cache per core
     * @param toByteMultiplier   multiplier... if shell report in KB (like with windows) it is simpler to multiply it wia this  constant not with parser directly
     * @return valid Cache information or null if query provider yields null
     * @throws Exception if something occurs (no info about cache aka no provider cant provide any info
     */
    private static CacheInfo platformNeutralQuery(ShellQueryProvider shellQueryProvider, ValueParser L4, ValueParser L3, ValueParser L2_FromSys, ValueParser L2_PerCoreEst, final int toByteMultiplier) throws Exception {
        final String data = shellQueryProvider.query();

        if (data == null)
            return null;
        AtomicReference<Exception> suppressed = new AtomicReference<>(null);
        final Function<ValueParser, Integer> safeCall =
                (parser) -> {
                    try {
                        Integer value = parser.parse(data);
                        return value == null || value <= 0 ? null : value * toByteMultiplier;
                    } catch (Exception e) {
                        if (suppressed.get() == null)
                            suppressed.set(e);
                        else
                            suppressed.get().addSuppressed(e);
                        return null;
                    }
                };
        CacheInfo out = new CacheInfo(safeCall.apply(L4), safeCall.apply(L3), safeCall.apply(L2_FromSys), safeCall.apply(L2_PerCoreEst));
        if (!out.hasSomeValues()) {
            if (suppressed.get() != null)
                throw suppressed.get();
            return null;
        }
        return out;
    }

    /**
     * runs your shell command
     *
     * @param shellCommand your command
     * @return Return result of your command
     * @throws IOException          throws when IO fails
     * @throws InterruptedException throws when interrupted
     */
    private static String runProcess(String... shellCommand) throws IOException, InterruptedException {
        return runProcess(false, shellCommand);
    }

    /**
     * runs your shell command
     *
     * @param ignoreReturnCode ignores return code.....
     * @param shellCommand     your command
     * @return Return result of your command
     * @throws IOException          throws when IO fails
     * @throws InterruptedException throws when interrupted
     */
    private static String runProcess(final boolean ignoreReturnCode, String... shellCommand) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(shellCommand);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitVal = process.waitFor();
        if (!ignoreReturnCode && exitVal != 0)
            throw new RuntimeException("shell command indicate unsuccessful operation via exit code " + exitVal);
        return output.toString();
    }


    /**
     * Runs your regex on data and extract a group from it and parse that group as integer
     *
     * @param dataIn your data
     * @param regex  your regex
     * @param group  your group
     * @return extracted value, may be null if not found
     */
    private static Integer extractFromStr(String dataIn, Pattern regex, int group) {
        Matcher matcher = regex.matcher(dataIn);
        return matcher.find() ? Integer.parseInt(matcher.group(group)) : null;
    }

    /**
     * Runs your regex on data and extract a group from it and parse that group as integer*
     * @param dataIn your data
     * @param regex  your regex
     * @param group  your group
     * @return extracted value, may be null if not found
     */
    private static Integer extractFromStr(String dataIn, String regex, int group) {
        return extractFromStr(dataIn, Pattern.compile(regex), group);
    }

    /**
     * Runs your regex on data and extract a 1st group from it and parse that group as integer*
     * @param dataIn your data
     * @param regex  your regex
     * @return extracted value, may be null if not found
     */

    private static Integer extractFromStr(String dataIn, String regex) {
        return extractFromStr(dataIn, regex, 1);
    }
}
