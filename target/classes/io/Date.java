package rm4j.io;

import java.io.Serializable;

public record Date(int year, int month, int date, int hrs, int min, int sec) implements Comparable<Date>, Cloneable, Serializable{

    private static final long serialVersionUID = 0x4434CD1E0808132AL;

    public Date(int year, int month, int date){
        this(year, month, date, 0, 0, 0);
    }

    @Override
    public int compareTo(Date d){
        int cmp;
        return ((cmp = Integer.compare(year, d.year)) == 0
            && (cmp = Integer.compare(month, d.month)) == 0
            && (cmp = Integer.compare(date, d.date)) == 0
            && (cmp = Integer.compare(hrs, d.hrs)) == 0
            && (cmp = Integer.compare(min, d.min)) == 0
            && (cmp = Integer.compare(sec, d.sec)) == 0)?
            0 : cmp;
    }

    @Override
    public String toString(){
        return "%d/%d/%d %02d:%02d:%02d".formatted(year, month, date, hrs, min, sec);
    }

    public String toDateString(){
        return "%d/%d/%d".formatted(year, month, date);
    }


    public String toStringForNamingUsage(){
        return "%d-%d-%d:%02d:%02d:%02d".formatted(year, month, date, hrs, min, sec);
    }

    @Override
    public Date clone(){
        return new Date(year, month, date, hrs, min, sec);
    }

    public Date previousMonth(){
        int year = this.year;
        int month = this.month;
        if(month == 1){
            year--;
            month = 12;
        }else{
            month--;
        }
        return new Date(year, month, date, hrs, min, sec);
    }

    public static int convertMonth(String s){
        return switch(s){
            case "Jan" -> 1;
            case "Feb" -> 2;
            case "Mar" -> 3;
            case "Apr" -> 4;
            case "May" -> 5;
            case "Jun" -> 6;
            case "Jul" -> 7;
            case "Aug" -> 8;
            case "Sep" -> 9;
            case "Oct" -> 10;
            case "Nov" -> 11;
            case "Dec" -> 12;
            default -> throw new NumberFormatException();
        };
    }

    public static Date parse(String s){
        String contents[] = s.split(" |:");
        if(contents.length != 8){
            throw new NumberFormatException();
        }
        return new Date(
            Integer.parseInt(contents[6]),
            convertMonth(contents[1]),
            Integer.parseInt(contents[2]),
            Integer.parseInt(contents[3]),
            Integer.parseInt(contents[4]),
            Integer.parseInt(contents[5]));
    }

}
