package org.junit.contrib.java.lang.system.internal;

import static com.github.stefanbirkner.fishbowl.Fishbowl.exceptionThrownBy;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileDescriptor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import com.github.stefanbirkner.fishbowl.Statement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class NoExitSecurityManagerTest {

	@RunWith(Parameterized.class)
	public static class tests_common_to_both_scenarios {
		private static final int DUMMY_STATUS = 1;

		@Parameters(name = "{0}")
		public static List<Object[]> data() {
			return asList(
				new Object[] {
					"with_original_SecurityManager",
					mock(SecurityManager.class)
				},
				new Object[] {
					"without_original_SecurityManager",
					null
				}
			);
		}

		private final NoExitSecurityManager securityManager;

		public tests_common_to_both_scenarios(
				String name, SecurityManager originalManager) {
			securityManager = new NoExitSecurityManager(originalManager);
		}

		@Test
		public void an_exception_with_the_status_is_thrown_when_checkExit_is_called() {
			CheckExitCalled exception = exceptionThrownBy(new Statement() {
				public void evaluate() {
					securityManager.checkExit(DUMMY_STATUS);
				}
			}, CheckExitCalled.class);
			assertThat(exception.getStatus()).isEqualTo(DUMMY_STATUS);
		}

		@Test
		public void information_about_a_missing_checkExit_call_is_available() {
			assertThat(securityManager.isCheckExitCalled()).isFalse();
		}

		@Test
		public void information_about_a_checkExit_call_is_available() {
			safeCallCheckExitWithStatus(DUMMY_STATUS);
			assertThat(securityManager.isCheckExitCalled()).isTrue();
		}

		@Test
		public void status_of_first_call_of_checkExit_is_available() {
			safeCallCheckExitWithStatus(DUMMY_STATUS);
			safeCallCheckExitWithStatus(DUMMY_STATUS + 1);
			assertThat(securityManager.getStatusOfFirstCheckExitCall())
				.isEqualTo(DUMMY_STATUS);
		}

		private void safeCallCheckExitWithStatus(int status) {
			try {
				securityManager.checkExit(status);
			} catch (CheckExitCalled ignored) {
			}
		}

		@Test
		public void fails_to_provide_status_of_first_checkExit_call_if_this_call_did_not_happen() {
			Throwable exception = exceptionThrownBy(new Statement() {
				public void evaluate() {
					securityManager.getStatusOfFirstCheckExitCall();
				}
			});
			assertThat(exception)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("checkExit(int) has not been called.");
		}
	}

	@RunWith(Parameterized.class)
	public static class public_methods_override {

		@Parameters(name = "{0}")
		public static List<Object[]> data() {
			List<Object[]> methods = new ArrayList<Object[]>();
			for (Method method : NoExitSecurityManager.class.getMethods())
				if (notDeclaredByObjectClass(method))
					methods.add(new Object[] { testName(method), method });
			return methods;
		}

		private static String testName(Method method) {
			return method.getName()
				+ "(" + join(method.getParameterTypes()) + ")";
		}

		private static String join(Class<?>[] types) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < types.length; i++) {
				if (i != 0)
					sb.append(",");
				sb.append(types[i].getSimpleName());
			}
			return sb.toString();
		}

		@Parameter(0)
		public String methodName;

		@Parameter(1)
		public Method method;

		@Test
		public void is_implemented_by_NoExitSecurityManager() {
			assertThat(method.getDeclaringClass())
				.isEqualTo(NoExitSecurityManager.class);
		}
	}

	public static class with_original_SecurityManager {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		private final SecurityManager originalSecurityManager = mock(SecurityManager.class);
		private final NoExitSecurityManager managerWithOriginal = new NoExitSecurityManager(
			originalSecurityManager);

		@Test
		public void getInCheck_is_delegated_to_original_security_manager() {
			when(originalSecurityManager.getInCheck()).thenReturn(true);
			assertThat(managerWithOriginal.getInCheck()).isTrue();
		}

		@Test
		public void security_context_of_original_security_manager_is_provided() {
			Object context = new Object();
			when(originalSecurityManager.getSecurityContext()).thenReturn(context);
			assertThat(managerWithOriginal.getSecurityContext()).isSameAs(context);
		}

		@Test
		public void checkPermission_without_context_is_delegated_to_original_security_manager() {
			Permission permission = mock(Permission.class);
			managerWithOriginal.checkPermission(permission);
			verify(originalSecurityManager).checkPermission(permission);
		}

		@Test
		public void checkPermission_with_context_is_delegated_to_original_security_manager() {
			Permission permission = mock(Permission.class);
			Object context = new Object();
			managerWithOriginal.checkPermission(permission, context);
			verify(originalSecurityManager).checkPermission(permission, context);
		}

		@Test
		public void checkCreateClassLoader_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkCreateClassLoader();
			verify(originalSecurityManager).checkCreateClassLoader();
		}

		@Test
		public void checkAccess_for_thread_is_delegated_to_original_security_manager() {
			Thread thread = mock(Thread.class);
			managerWithOriginal.checkAccess(thread);
			verify(originalSecurityManager).checkAccess(thread);
		}

		@Test
		public void checkAccess_for_thread_group_is_delegated_to_original_security_manager() {
			ThreadGroup threadGroup = mock(ThreadGroup.class);
			managerWithOriginal.checkAccess(threadGroup);
			verify(originalSecurityManager).checkAccess(threadGroup);
		}

		@Test
		public void checkExec_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkExec("arbitrary cmd");
			verify(originalSecurityManager).checkExec("arbitrary cmd");
		}

		@Test
		public void checkLink_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkLink("arbitrary lib");
			verify(originalSecurityManager).checkLink("arbitrary lib");
		}

		@Test
		public void checkRead_for_file_descriptor_is_delegated_to_original_security_manager() {
			FileDescriptor fileDescriptor = new FileDescriptor();
			managerWithOriginal.checkRead(fileDescriptor);
			verify(originalSecurityManager).checkRead(fileDescriptor);
		}

		@Test
		public void checkRead_for_filename_without_context_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkRead("arbitrary file");
			verify(originalSecurityManager).checkRead("arbitrary file");
		}

		@Test
		public void checkRead_for_filename_with_context_is_delegated_to_original_security_manager() {
			Object context = new Object();
			managerWithOriginal.checkRead("arbitrary file", context);
			verify(originalSecurityManager).checkRead("arbitrary file", context);
		}

		@Test
		public void checkWrite_of_file_descriptor_is_delegated_to_original_security_manager() {
			FileDescriptor fileDescriptor = new FileDescriptor();
			managerWithOriginal.checkWrite(fileDescriptor);
			verify(originalSecurityManager).checkWrite(fileDescriptor);
		}

		@Test
		public void checkWrite_of_filename_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkWrite("arbitrary file");
			verify(originalSecurityManager).checkWrite("arbitrary file");
		}

		@Test
		public void checkDelete_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkDelete("arbitrary file");
			verify(originalSecurityManager).checkDelete("arbitrary file");
		}

		@Test
		public void checkConnect_without_context_is_delegated_to_original_security_manager() {
			int port = 234;
			managerWithOriginal.checkConnect("host", port);
			verify(originalSecurityManager).checkConnect("host", port);
		}

		@Test
		public void checkConnect_with_context_is_delegated_to_original_security_manager() {
			int port = 234;
			Object context = new Object();
			managerWithOriginal.checkConnect("host", port, context);
			verify(originalSecurityManager).checkConnect("host", port, context);
		}

		@Test
		public void checkListen_is_delegated_to_original_security_manager() {
			int port = 234;
			managerWithOriginal.checkListen(port);
			verify(originalSecurityManager).checkListen(port);
		}

		@Test
		public void checkAccept_is_delegated_to_original_security_manager() {
			int port = 234;
			managerWithOriginal.checkAccept("host", port);
			verify(originalSecurityManager).checkAccept("host", port);
		}

		@Test
		public void checkMulticast_without_TTL_is_delegated_to_original_security_manager() {
			InetAddress inetAddress = mock(InetAddress.class);
			managerWithOriginal.checkMulticast(inetAddress);
			verify(originalSecurityManager).checkMulticast(inetAddress);
		}

		@Test
		public void checkMulticast_with_TTL_is_delegated_to_original_security_manager() {
			InetAddress inetAddress = mock(InetAddress.class);
			byte ttl = 24;
			managerWithOriginal.checkMulticast(inetAddress, ttl);
			verify(originalSecurityManager).checkMulticast(inetAddress, ttl);
		}

		@Test
		public void checkPropertiesAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkPropertiesAccess();
			verify(originalSecurityManager).checkPropertiesAccess();
		}

		@Test
		public void checkPropertyAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkPropertyAccess("arbitrary key");
			verify(originalSecurityManager).checkPropertyAccess("arbitrary key");
		}

		@Test
		public void checkTopLevelWindow_is_delegated_to_original_security_manager() {
			Object window = new Object();
			when(originalSecurityManager.checkTopLevelWindow(window)).thenReturn(true);
			assertThat(managerWithOriginal.checkTopLevelWindow(window)).isTrue();
		}

		@Test
		public void checkPrintJobAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkPrintJobAccess();
			verify(originalSecurityManager).checkPrintJobAccess();
		}

		@Test
		public void checkSystemClipboardAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkSystemClipboardAccess();
			verify(originalSecurityManager).checkSystemClipboardAccess();
		}

		@Test
		public void checkAwtEventQueueAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkAwtEventQueueAccess();
			verify(originalSecurityManager).checkAwtEventQueueAccess();
		}

		@Test
		public void checkPackageAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkPackageAccess("arbitrary package");
			verify(originalSecurityManager).checkPackageAccess("arbitrary package");
		}

		@Test
		public void checkPackageDefinition_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkPackageDefinition("arbitrary package");
			verify(originalSecurityManager).checkPackageDefinition("arbitrary package");
		}

		@Test
		public void checkSetFactory_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkSetFactory();
			verify(originalSecurityManager).checkSetFactory();
		}

		@Test
		public void checkMemberAccess_is_delegated_to_original_security_manager() {
			Class<?> arbitraryClass = Integer.class;
			int which = 394;
			managerWithOriginal.checkMemberAccess(arbitraryClass, which);
			verify(originalSecurityManager).checkMemberAccess(arbitraryClass, which);
		}

		@Test
		public void checkSecurityAccess_is_delegated_to_original_security_manager() {
			managerWithOriginal.checkSecurityAccess("arbitrary target");
			verify(originalSecurityManager).checkSecurityAccess("arbitrary target");
		}

		@Test
		public void thread_group_of_original_security_manager_is_provided() {
			ThreadGroup threadGroup = new ThreadGroup("dummy name");
			when(originalSecurityManager.getThreadGroup()).thenReturn(threadGroup);
			assertThat(managerWithOriginal.getThreadGroup()).isSameAs(threadGroup);
		}
	}

	public static class without_original_SecurityManager {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		private final NoExitSecurityManager managerWithoutOriginal = new NoExitSecurityManager(null);

		@Test
		public void getInCheck_returns_false() {
			assertThat(managerWithoutOriginal.getInCheck()).isFalse();
		}

		@Test
		public void getSecurityContext_may_be_called() {
			managerWithoutOriginal.getSecurityContext();
		}

		@Test
		public void checkPermission_without_context_may_be_called() {
			Permission permission = mock(Permission.class);
			managerWithoutOriginal.checkPermission(permission);
		}

		@Test
		public void checkPermission_with_context_may_be_called() {
			Permission permission = mock(Permission.class);
			Object context = new Object();
			managerWithoutOriginal.checkPermission(permission, context);
		}

		@Test
		public void checkCreateClassLoader_may_be_called() {
			managerWithoutOriginal.checkCreateClassLoader();
		}

		@Test
		public void checkAccess_for_thread_may_be_called() {
			Thread thread = mock(Thread.class);
			managerWithoutOriginal.checkAccess(thread);
		}

		@Test
		public void checkAccess_for_thread_group_may_be_called() {
			ThreadGroup threadGroup = mock(ThreadGroup.class);
			managerWithoutOriginal.checkAccess(threadGroup);
		}

		@Test
		public void checkExec_may_be_called() {
			managerWithoutOriginal.checkExec("dummy cmd");
		}

		@Test
		public void checkLink_may_be_called() {
			managerWithoutOriginal.checkLink("dummy lib");
		}

		@Test
		public void checkRead_for_file_descriptor_may_be_called() {
			FileDescriptor fileDescriptor = new FileDescriptor();
			managerWithoutOriginal.checkRead(fileDescriptor);
		}

		@Test
		public void checkRead_for_filename_without_context_may_be_called() {
			managerWithoutOriginal.checkRead("dummy file");
		}

		@Test
		public void checkRead_for_fukebane_with_context_may_be_called() {
			Object context = new Object();
			managerWithoutOriginal.checkRead("dummy file", context);
		}

		@Test
		public void checkWrite_of_file_descriptor_may_be_called() {
			FileDescriptor fileDescriptor = new FileDescriptor();
			managerWithoutOriginal.checkWrite(fileDescriptor);
		}

		@Test
		public void checkWrite_of_filename_may_be_called() {
			managerWithoutOriginal.checkWrite("dummy file");
		}

		@Test
		public void checkDelete_may_be_called() {
			managerWithoutOriginal.checkDelete("dummy file");
		}

		@Test
		public void checkConnect_without_context_may_be_called() {
			int port = 234;
			managerWithoutOriginal.checkConnect("host", port);
		}

		@Test
		public void checkConnect_with_context_may_be_called() {
			int port = 234;
			Object context = new Object();
			managerWithoutOriginal.checkConnect("host", port, context);
		}

		@Test
		public void checkListen_may_be_called() {
			int port = 234;
			managerWithoutOriginal.checkListen(port);
		}

		@Test
		public void checkAccept_may_be_called() {
			int port = 234;
			managerWithoutOriginal.checkAccept("host", port);
		}

		@Test
		public void checkMulticast_without_TTL_may_be_called() {
			InetAddress inetAddress = mock(InetAddress.class);
			managerWithoutOriginal.checkMulticast(inetAddress);
		}

		@Test
		public void checkMulticast_with_TTL_may_be_called() {
			InetAddress inetAddress = mock(InetAddress.class);
			byte ttl = 24;
			managerWithoutOriginal.checkMulticast(inetAddress, ttl);
		}

		@Test
		public void checkPropertiesAccess_may_be_called() {
			managerWithoutOriginal.checkPropertiesAccess();
		}

		@Test
		public void checkPropertyAccess_may_be_called() {
			managerWithoutOriginal.checkPropertyAccess("dummy key");
		}

		@Test
		public void checkTopLevelWindow_may_be_called() {
			Object window = new Object();
			managerWithoutOriginal.checkTopLevelWindow(window);
		}

		@Test
		public void checkPrintJobAccess_may_be_called() {
			managerWithoutOriginal.checkPrintJobAccess();
		}

		@Test
		public void checkSystemClipboardAccess_may_be_called() {
			managerWithoutOriginal.checkSystemClipboardAccess();
		}

		@Test
		public void checkAwtEventQueueAccess_may_be_called() {
			managerWithoutOriginal.checkAwtEventQueueAccess();
		}

		@Test
		public void checkPackageAccess_may_be_called() {
			managerWithoutOriginal.checkPackageAccess("dummy package");
		}

		@Test
		public void checkPackageDefinition_may_be_called() {
			managerWithoutOriginal.checkPackageDefinition("dummy package");
		}

		@Test
		public void checkSetFactory_may_be_called() {
			managerWithoutOriginal.checkSetFactory();
		}

		@Test
		public void checkMemberAccess_may_be_called() {
			Class<?> arbitraryClass = Integer.class;
			int which = 394;
			managerWithoutOriginal.checkMemberAccess(arbitraryClass, which);
		}

		@Test
		public void checkSecurityAccess_may_be_called() {
			managerWithoutOriginal.checkSecurityAccess("arbitrary target");
		}

		@Test
		public void getThreadGroup_may_be_called() {
			managerWithoutOriginal.getThreadGroup();
		}
	}

	private static boolean notDeclaredByObjectClass(Method method) {
		return !method.getDeclaringClass().equals(Object.class);
	}
}
