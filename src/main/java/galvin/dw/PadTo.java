package galvin.dw;

import java.util.ArrayList;
import java.util.List;

public class PadTo {
    /**
     * Pads a string with ' ' until it is the given minimum length.
     *
     * If the string is already long enough, it will not be modified.
     *
     * @param text
     * @param minLength
     * @return
     */
    public static String padTo( String text, int minLength ){
        StringBuilder result = new StringBuilder( minLength );
        int remainder = minLength;

        if( text != null){
            int length = text.length();
            if( length <= minLength ){
                remainder = minLength - length;
            }
            else{
                remainder = 0;
            }

            result.append(text);
        }

        for( int i = 0; i < remainder; i++ ){
            result.append(' ');
        }

        return result.toString();
    }

    public static List<String> padTo( List<String> strings ){
        int count = strings.size();
        List<String> result = new ArrayList<>(count);

        int length = 0;
        for( String string : strings ){
            if( string != null ){
                length = Math.max( length, string.length() );
            }
        }

        for( String string : strings ){
            result.add( padTo( string, length ) );
        }

        return result;
    }

    public static String paddedLayout( char tableHeaderSeparator, List<String> ... lists ){
        String padded = paddedLayout( lists );
        StringBuilder separator = new StringBuilder();

        String[] lines = padded.split("\n");
        if( lines.length > 0 ){
            int length = lines[0].length();
            for( int i = 0; i < length; i++ ){
                separator.append(tableHeaderSeparator);
            }

            StringBuilder result = new StringBuilder();
            result.append( lines[0] );
            result.append( "\n" );
            result.append( separator );
            result.append( "\n" );

            for( int i = 1; i < lines.length; i++ ){
                result.append( lines[i] );

                if( i+1 < lines.length ){
                    result.append( "\n" );
                }
            }
            return result.toString();
        }

        return padded;
    }

    public static String paddedLayout( List<String> ... lists ){
        // copy the list to a clone. we're gonna be playing around with it,
        // and this prevents side effects
        List<String>[] clone = new List[lists.length];
        for( int i = 0; i < clone.length; i++ ){
            List _list = new ArrayList<String>( lists[i].size() );
            _list.addAll( lists[i] );

            clone[i] = _list;
        }

        //calculate the columns and rows of the table
        int columns = clone.length;
        int rows = 0;
        for( List<String> list : clone ){
            rows = Math.max( rows, list.size() );
        }

        //make sure every list is the same length
        for( List<String> list : clone ){
            if( list.size() < rows ){
                for( int i = list.size(); i < rows; i++ ){
                    list.add(" ");
                }
            }
        }

        //create padded text for every column
        List<String>[] padded = new List[columns];
        int index = 0;
        for( List<String> list : clone ){
            padded[index] = padTo(list);
            index++;
        }

        //create the table

        StringBuilder result = new StringBuilder();

        for( int row = 0; row < rows; row++ ){
            for( int column = 0; column < columns; column++ ){
                List<String> columnStrings = padded[column];
                result.append( columnStrings.get(row) );

                if( column+1 < columns ){
                    result.append( " " );
                }
                else if( row+1 < rows ){
                    result.append( "\n" );
                }
            }
        }

        return result.toString();
    }
}
