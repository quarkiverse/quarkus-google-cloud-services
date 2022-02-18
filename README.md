# Quarkiverse - Quarkus Google Cloud Services
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-9-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
[![version](https://img.shields.io/maven-central/v/io.quarkiverse.googlecloudservices/quarkus-google-cloud-services-bom)](https://repo1.maven.org/maven2/io/quarkiverse/googlecloudservices/)
[![Build](https://github.com/quarkiverse/quarkus-google-cloud-services/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkus-google-cloud-services/actions?query=workflow%3ABuild)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This repository hosts Quarkus extensions for different Google Cloud Services.

You can find the documentation in the [Google Cloud Services Quarkiverse documentation site](https://quarkiverse.github.io/quarkiverse-docs/quarkus-google-cloud-services/main/).

The following services are implemented:
- [BigQuery](bigquery)
- [Bigtable](bigtable)
- [Firestore](firestore)
- [PubSub](pubsub)
- [Secret Manager](secret-manager)
- [Spanner](spanner)
- [Storage](storage)
    
## Example applications

Example applications can be found inside the integration-test folder:
- [main](integration-tests/main): RESTEasy endpoints using all the Google Cloud Services extensions, to be deployed as a standalone JAR.
- [google-cloud-functions](integration-tests/google-cloud-functions): A Google Cloud HTTP function using Google Cloud Storage. 
- [app-engine](integration-tests/app-engine): A RESTEasy endpoint using Google Cloud Storage, to be deployed inside Google App Engine.
    
## Contributing

Contributions are always welcome, but better create an issue to discuss them prior to any contributions.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.loicmathieu.fr"><img src="https://avatars2.githubusercontent.com/u/1819009?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Loïc Mathieu</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=loicmathieu" title="Code">💻</a> <a href="#maintenance-loicmathieu" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/sberyozkin"><img src="https://avatars3.githubusercontent.com/u/467639?v=4?s=100" width="100px;" alt=""/><br /><sub><b>sberyozkin</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=sberyozkin" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/dzou"><img src="https://avatars1.githubusercontent.com/u/3209274?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Daniel Zou</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=dzou" title="Code">💻</a></td>
    <td align="center"><a href="http://ynagai.info"><img src="https://avatars1.githubusercontent.com/u/1780156?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Yuki Nagai</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=uny" title="Documentation">📖</a></td>
    <td align="center"><a href="http://madsopheim.com"><img src="https://avatars.githubusercontent.com/u/1844557?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Mads Opheim</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=madsop" title="Code">💻</a> <a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=madsop" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/PeterUlb"><img src="https://avatars.githubusercontent.com/u/13261215?v=4?s=100" width="100px;" alt=""/><br /><sub><b>PeterUlb</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=PeterUlb" title="Code">💻</a></td>
    <td align="center"><a href="https://www.4pixel.it"><img src="https://avatars.githubusercontent.com/u/3707628?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Felipe Sabadini</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=felipesabadini" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://twitter.com/ppalaga"><img src="https://avatars.githubusercontent.com/u/1826249?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Peter Palaga</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=ppalaga" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/lemoigne-yt"><img src="https://avatars.githubusercontent.com/u/94898819?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Yann-Thomas LE MOIGNE</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=lemoigne-yt" title="Documentation">📖</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
