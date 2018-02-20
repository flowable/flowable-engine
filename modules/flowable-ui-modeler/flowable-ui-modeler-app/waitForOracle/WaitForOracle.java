import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class WaitForOracle
{
  public static void main(String[] argv)
  {
    System.out.println("-------- Oracle JDBC Connection Testing ------");
    String oracleUrl = System.getenv("datasource.url");
    if (oracleUrl == null || oracleUrl.trim().equalsIgnoreCase(""))
    {
      oracleUrl = System.getenv("spring.datasource.url");
      if (oracleUrl == null || oracleUrl.trim().equalsIgnoreCase(""))
      {
        System.out.println("What's your Oracle URL?");
        System.exit(1);
      }
      oracleUrl = oracleUrl.replaceAll("rdsoracle", "oracle");
    }

    System.out.println(oracleUrl);

    try
    {
      Class.forName("oracle.jdbc.driver.OracleDriver");
    }
    catch (ClassNotFoundException e)
    {
      System.out.println("Where is your Oracle JDBC Driver?");
      e.printStackTrace();
      System.exit(2);

    }

    System.out.println("Oracle JDBC Driver Registered!");

    Connection connection = null;
    try
    {
      connection = DriverManager.getConnection(oracleUrl, "nonexist", "nonexist");
    }
    catch (SQLException e)
    {
      if (e.getMessage().indexOf("ORA-01017") >= 0)  // when oracle is ready, it will throw 'ORA-01017 Invalid Username/Password'
      {
        System.exit(0);
      } else {
        System.exit(3);
        System.out.println("Oracle is not ready!");
        e.printStackTrace();
      }

    }

  }
}