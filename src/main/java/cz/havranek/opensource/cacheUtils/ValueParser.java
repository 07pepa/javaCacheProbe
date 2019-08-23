package cz.havranek.opensource.cacheUtils;
@FunctionalInterface
interface ValueParser {
  Integer parse(String data) throws Exception;
}
