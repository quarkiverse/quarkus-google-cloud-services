# Quarkiverse - Google Cloud Services
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

This repository hosts extensions for different Google Cloud Services.

You can find documentation for each of these extensions in their sub-directory:
- [BigQuery](bigquery)
- [Firestore](firestore)
- [PubSub](pubsub)
- [Spanner](spanner)
- [Storage](storage)

They all share an optional common configuration property to set the project ID:
```
quarkus.google.cloud.projectId=<your-project-id>
```

## Authenticating to Google Cloud

There are several ways to authenticate to Google Cloud, 
it depends from where your application runs (inside our outside Google Cloud Platform) and for which service.

The current authentication flow is as follows:
- Check the `quarkus.google.cloud.service-account-location` property, if it exists, use the service account file from this location.
- Use `GoogleCredentials.getApplicationDefault()` that will search for credentials in multiple places:
    - Credentials file pointed to by the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.
    - Credentials provided by the Google Cloud SDK `gcloud auth application-default login` command.
    - Google Cloud managed environment (AppEngine, GCE, ...) built-in credentials.
    
## WARNING

This project is still in its early stage and no releases has been done yet. 
The steps described inside the documentation to create a project cannot be done, one must package and install the dependencies locally to test it.

Contributions are always welcome, but this repository is not really ready for external contributions yet, better create an issue
to discuss them prior to any contributions.

As already said, there is no release yet. To use it, you must build it from the source using Maven.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.loicmathieu.fr"><img src="https://avatars2.githubusercontent.com/u/1819009?v=4" width="100px;" alt=""/><br /><sub><b>Loïc Mathieu</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-google-cloud-services/commits?author=loicmathieu" title="Code">💻</a> <a href="#maintenance-loicmathieu" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!