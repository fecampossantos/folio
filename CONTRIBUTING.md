# Contributing to Folio

Thank you for your interest in contributing to **Folio**. We welcome bug reports, feature suggestions, documentation improvements, and code contributions.

---

## How to Contribute

### 1. Reporting Bugs
- Search existing issues to ensure your bug hasn't been reported.
- Open a new issue with a descriptive title, step-by-step reproduction instructions, Android OS version, and logcat trace if available.

### 2. Suggesting Enhancements
- Check existing feature requests.
- Open a feature request issue detailing your proposed enhancement and user experience benefit.

### 3. Submitting Code (Pull Requests)
- Fork the repository and create a descriptive branch:
  ```bash
  git checkout -b feature/my-new-feature
  ```
- Follow Kotlin coding conventions and ensure all functions include KDoc documentation comments.
- Run unit tests locally before submitting:
  ```bash
  ./gradlew test
  ```
- Submit a Pull Request targeting the `main` branch with a summary of changes.

---

## Code Style Guidelines

- **Architecture**: Keep code clean using MVVM + Repository pattern.
- **UI Framework**: Use Jetpack Compose (Material 3).
- **Documentation**: All public and internal functions must include clear KDoc docstrings (`/** ... */`).
- **Tests**: Ensure new code features include corresponding unit tests under `app/src/test/java/`.

---

## License

By contributing to Folio, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
