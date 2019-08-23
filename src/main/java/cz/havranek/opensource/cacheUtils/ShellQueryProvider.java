package cz.havranek.opensource.cacheUtils;
@FunctionalInterface
interface ShellQueryProvider {
    String query() throws Exception;
}
