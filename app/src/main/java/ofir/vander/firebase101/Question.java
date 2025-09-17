package ofir.vander.firebase101;

public class Question {
    private int level;
    private String id, queText, ansCorrect, ansWrong1, ansWrong2, ansWrong3;

    public Question() {
    }

    public Question(String id, int level, String queText, String ansCorrect, String andWrong1, String andWrong2, String ansWrong3) {
        this.id = id;
        this.level = level;
        this.queText = queText;
        this.ansCorrect = ansCorrect;
        this.ansWrong1 = andWrong1;
        this.ansWrong2 = andWrong2;
        this.ansWrong3 = ansWrong3;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getQueText() {
        return queText;
    }

    public void setQueText(String queText) {
        this.queText = queText;
    }

    public String getAnsCorrect() {
        return ansCorrect;
    }

    public void setAnsCorrect(String ansCorrect) {
        this.ansCorrect = ansCorrect;
    }

    public String getAnsWrong1() {
        return ansWrong1;
    }

    public void setAnsWrong1(String ansWrong1) {
        this.ansWrong1 = ansWrong1;
    }

    public String getAnsWrong2() {
        return ansWrong2;
    }

    public void setAnsWrong2(String ansWrong2) {
        this.ansWrong2 = ansWrong2;
    }

    public String getAnsWrong3() {
        return ansWrong3;
    }

    public void setAnsWrong3(String ansWrong3) {
        this.ansWrong3 = ansWrong3;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id + '\'' +
                ", queText=\'" + queText +
                '}';
    }
}

