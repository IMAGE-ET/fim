@rem -------------------------------------------------------------------------------------------------------------------
@rem This file is part of Fim - File Integrity Manager
@rem
@rem Copyright (C) 2015  Etienne Vrignaud
@rem
@rem Fim is free software: you can redistribute it and/or modify
@rem it under the terms of the GNU General Public License as published by
@rem the Free Software Foundation, either version 3 of the License, or
@rem (at your option) any later version.
@rem
@rem Fim is distributed in the hope that it will be useful,
@rem but WITHOUT ANY WARRANTY; without even the implied warranty of
@rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@rem GNU General Public License for more details.
@rem
@rem You should have received a copy of the GNU General Public License
@rem along with Fim.  If not, see <http://www.gnu.org/licenses/>.
@rem -------------------------------------------------------------------------------------------------------------------

@echo off
set "baseDir=%~dp0%"

set JAVA_OPTIONS=-Xmx4g -XX:MaxMetaspaceSize=4g

java %JAVA_OPTIONS% -jar "%baseDir%bin\fim-${project.version}.jar" "%1" "%2" "%3" "%4" "%5" "%6"
