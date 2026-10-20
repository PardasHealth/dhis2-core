package org.hisp.dhis.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class JdbcAuditRepositoryCompressionTest {
  @Test
  void compressedAuditDataRoundTrips() throws Exception {
    byte[] compressed = (byte[]) invoke("compress", String.class, "audit payload");
    assertEquals("audit payload", invoke("decompress", byte[].class, compressed));
  }

  @Test
  void corruptCompressedAuditDataThrowsInsteadOfReturningNull() throws Exception {
    InvocationTargetException error =
        assertThrows(
            InvocationTargetException.class,
            () -> invoke("decompress", byte[].class, new byte[] {1, 2, 3}));
    assertInstanceOf(IllegalStateException.class, error.getCause());
  }

  private static Object invoke(String methodName, Class<?> argumentType, Object argument)
      throws Exception {
    Method method = JdbcAuditRepository.class.getDeclaredMethod(methodName, argumentType);
    method.setAccessible(true);
    return method.invoke(null, argument);
  }
}
