# Quarkiverse - Quarkus Google Cloud Services
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-22-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
[![version](https://img.shields.io/maven-central/v/io.quarkiverse.googlecloudservices/quarkus-google-cloud-services-bom)](https://repo1.maven.org/maven2/io/quarkiverse/googlecloudservices/)
[![Build](https://github.com/quarkiverse/quarkus-google-cloud-services/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkus-google-cloud-services/actions?query=workflow%3ABuild)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This repository hosts Quarkus extensions for different Google Cloud Services.

You can find the documentation in the [Google Cloud Services Quarkiverse documentation site](https://quarkiverse.github.io/quarkiverse-docs/quarkus-google-cloud-services/main/).

The following services are implemented:
- [BigQuery](bigquery)
- [Bigtable](bigtable)
- [Firebase Admin](firebase-admin)
- [Firestore](firestore)
- [PubSub](pubsub)
- [Secret Manager](secret-manager)
- [Spanner](spanner)
- [Storage](storage)
- [Logging](logging)
    
## Example applications

Example applications can be found inside the integration-test folder:
- [main](integration-tests/main): RESTEasy endpoints using all the Google Cloud Services extensions, to be deployed as a standalone JAR.
- [google-cloud-functions](integration-tests/google-cloud-functions): A Google Cloud HTTP function using Google Cloud Storage. 
- [app-engine](integration-tests/app-engine): A RESTEasy endpoint using Google Cloud Storage, to be deployed inside Google App Engine.
- [firebase-admin](integration-tests/firebase-admin): RESTEasy endpoints using Firebase Admin SDK features, such as user management.
    
## Contributing

Contributions are always welcome, but better create an issue to discuss them prior to any contributions.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://www.loicmathieu.fr"><img src="https://avatars2.githubusercontent.com/u/1819009?v=4?s=100" width="100px;" alt="Loïc Mathieu"/><br /><sub><b>Loïc Mathieu</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=loicmathieu" title="Code">💻</a> <a href="#maintenance-loicmathieu" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/sberyozkin"><img src="https://avatars3.githubusercontent.com/u/467639?v=4?s=100" width="100px;" alt="sberyozkin"/><br /><sub><b>sberyozkin</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=sberyozkin" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/dzou"><img src="https://avatars1.githubusercontent.com/u/3209274?v=4?s=100" width="100px;" alt="Daniel Zou"/><br /><sub><b>Daniel Zou</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=dzou" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://ynagai.info"><img src="https://avatars1.githubusercontent.com/u/1780156?v=4?s=100" width="100px;" alt="Yuki Nagai"/><br /><sub><b>Yuki Nagai</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=uny" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://madsopheim.com"><img src="https://avatars.githubusercontent.com/u/1844557?v=4?s=100" width="100px;" alt="Mads Opheim"/><br /><sub><b>Mads Opheim</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=madsop" title="Code">💻</a> <a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=madsop" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/PeterUlb"><img src="https://avatars.githubusercontent.com/u/13261215?v=4?s=100" width="100px;" alt="PeterUlb"/><br /><sub><b>PeterUlb</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=PeterUlb" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.4pixel.it"><img src="https://avatars.githubusercontent.com/u/3707628?v=4?s=100" width="100px;" alt="Felipe Sabadini"/><br /><sub><b>Felipe Sabadini</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=felipesabadini" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://twitter.com/ppalaga"><img src="https://avatars.githubusercontent.com/u/1826249?v=4?s=100" width="100px;" alt="Peter Palaga"/><br /><sub><b>Peter Palaga</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=ppalaga" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/yatho"><img src="https://avatars.githubusercontent.com/u/6213245?v=4?s=100" width="100px;" alt="Yann-Thomas LE MOIGNE"/><br /><sub><b>Yann-Thomas LE MOIGNE</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=yatho" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://lesincroyableslivres.fr/"><img src="https://avatars.githubusercontent.com/u/1279749?v=4?s=100" width="100px;" alt="Guillaume Smet"/><br /><sub><b>Guillaume Smet</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=gsmet" title="Documentation">📖</a> <a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=gsmet" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/lucaspouzac"><img src="https://avatars.githubusercontent.com/u/758899?v=4?s=100" width="100px;" alt="Lucas Pouzac"/><br /><sub><b>Lucas Pouzac</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=lucaspouzac" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/bernardocoferre"><img src="https://avatars.githubusercontent.com/u/4994556?v=4?s=100" width="100px;" alt="Bernardo Coferre"/><br /><sub><b>Bernardo Coferre</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=bernardocoferre" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zanmagerl"><img src="https://avatars.githubusercontent.com/u/36709679?v=4?s=100" width="100px;" alt="Žan Magerl"/><br /><sub><b>Žan Magerl</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=zanmagerl" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.larsan.net"><img src="https://avatars.githubusercontent.com/u/30407653?v=4?s=100" width="100px;" alt="Lars J. Nilsson"/><br /><sub><b>Lars J. Nilsson</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=Fungrim" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/nahguam"><img src="https://avatars.githubusercontent.com/u/4394757?v=4?s=100" width="100px;" alt="Dave Maughan"/><br /><sub><b>Dave Maughan</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=nahguam" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zZHorizonZz"><img src="https://avatars.githubusercontent.com/u/60035933?v=4?s=100" width="100px;" alt="Daniel Fiala"/><br /><sub><b>Daniel Fiala</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=zZHorizonZz" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://jamesnetherton.github.io/"><img src="https://avatars.githubusercontent.com/u/4721408?v=4?s=100" width="100px;" alt="James Netherton"/><br /><sub><b>James Netherton</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=jamesnetherton" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.radcortez.com"><img src="https://avatars.githubusercontent.com/u/5796305?v=4?s=100" width="100px;" alt="Roberto Cortez"/><br /><sub><b>Roberto Cortez</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=radcortez" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/geoand"><img src="https://avatars.githubusercontent.com/u/4374975?v=4?s=100" width="100px;" alt="Georgios Andrianakis"/><br /><sub><b>Georgios Andrianakis</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=geoand" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/snazy"><img src="https://avatars.githubusercontent.com/u/957468?v=4?s=100" width="100px;" alt="Robert Stupp"/><br /><sub><b>Robert Stupp</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=snazy" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://uk.linkedin.com/in/hchigadani"><img src="https://avatars.githubusercontent.com/u/12896715?v=4?s=100" width="100px;" alt="Hemantkumar Chigadani"/><br /><sub><b>Hemantkumar Chigadani</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=Hemantkumar-Chigadani" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/wabrit"><img src="https://avatars.githubusercontent.com/u/4264910?v=4?s=100" width="100px;" alt="wabrit"/><br /><sub><b>wabrit</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-google-cloud-services/commits?author=wabrit" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
