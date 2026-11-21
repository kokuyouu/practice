package core;

public final class TextValues {
    private TextValues() {}

    public static boolean isBlank(String value) {
        return value.codePoints()
                .allMatch(
                        code ->
                                Character.isWhitespace(code)
                                        || Character.isSpaceChar(code)
                                        || code == 0x85);
    }

    public static int compare(String left, String right) {
        var leftPoints = left.codePoints().iterator();
        var rightPoints = right.codePoints().iterator();
        while (leftPoints.hasNext() && rightPoints.hasNext()) {
            int result = Integer.compare(leftPoints.nextInt(), rightPoints.nextInt());
            if (result != 0) {
                return result;
            }
        }
        return Boolean.compare(leftPoints.hasNext(), rightPoints.hasNext());
    }
}
