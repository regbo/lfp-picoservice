package de.adito.picoservice.test;

import java.sql.Date;
import java.util.Objects;

import de.adito.picoservice.IPicoRegistration;

public interface EqualsTest {

	public static void main(String[] args) {
		TestRegistration v1 = new TestRegistration();
		TestRegistration v2 = new TestRegistration();
		System.out.println(v1.equals(v2));
	}

	static class TestRegistration implements IPicoRegistration {

		@Override
		public Class<?> getAnnotatedClass() {
			return Date.class;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.getClass(), getAnnotatedClass());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestRegistration other = (TestRegistration) obj;
			return Objects.equals(getAnnotatedClass(), other.getAnnotatedClass());
		}

	}
}
